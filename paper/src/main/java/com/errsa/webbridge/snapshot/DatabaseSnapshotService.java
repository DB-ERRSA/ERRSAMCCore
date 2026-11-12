package com.errsa.webbridge.snapshot;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Read-only snapshot adapter for the existing Guilds and PlayerQuests schemas. */
final class DatabaseSnapshotService {
    private static final String GUILDS_SQL =
            "SELECT g.id,g.name,g.tag,g.leader_uuid,g.level,g.total_xp,g.created_at, " +
            "COALESCE(p.description,'') description,COALESCE(p.color,'AQUA') color, " +
            "COALESCE(p.open_membership,0) open_membership, COALESCE(p.banner_material,'WHITE_BANNER') " +
            "banner_material, COALESCE(p.banner_data,'') banner_data,COALESCE(b.balance,0) bank_balance, " +
            "(SELECT COUNT(*) FROM guild_members m WHERE m.guild_id=g.id) member_count, COALESCE((SELECT " +
            "SUM((ABS(c2.x2-c2.x1)+1)*(ABS(c2.z2-c2.z1)+1)) FROM guild_claims c2 WHERE c2.guild_id=g.id),0) " +
            "claim_area, COALESCE((SELECT SUM(x.amount) FROM guild_xp_ledger x WHERE x.guild_id=g.id AND " +
            "datetime(x.created_at)>=datetime('now','-7 days')),0) weekly_xp, COALESCE((SELECT sp.points " +
            "FROM guild_season_points sp JOIN guild_seasons s ON s.id=sp.season_id WHERE sp.guild_id=g.id " +
            "AND s.status='ACTIVE' ORDER BY s.id DESC LIMIT 1),0) season_points FROM guilds g LEFT JOIN " +
            "guild_profiles p ON p.guild_id=g.id LEFT JOIN guild_bank_accounts b ON b.guild_id=g.id ORDER BY " +
            "g.total_xp DESC";
    private static final String QUESTS_SQL =
            "SELECT id,creator,claimer,type,status,title,required_item,required_amount," +
            "deposited_amount,reward_money,worker_deposit,created_at,claimed_at,expires_at," +
            "description,required_material,submitted_at,completed_at,world,x1,y1,z1,x2,y2,z2," +
            "category,target,audience,guild_id,guild_name,guild_split,reward_funding," +
            "funding_guild_id,funding_guild_name,blueprint_sx,blueprint_sy,blueprint_sz FROM " +
            "quests ORDER BY created_at DESC LIMIT 500";

    Map<String, Object> guildSnapshot(File database) throws Exception {
        Map<String, Object> snapshot = availability(database);
        if (!database.isFile()) {
            return snapshot;
        }
        try (Connection connection = openReadOnly(database)) {
            List<Map<String, Object>> guilds = rows(connection, GUILDS_SQL);
            for (Map<String, Object> guild : guilds) {
                String id = sqlQuote(String.valueOf(guild.get("id")));
                guild.put("members", rows(connection,
                        "SELECT player_uuid,rank,joined_at FROM guild_members WHERE guild_id="
                                + id + " ORDER BY CASE rank WHEN 'LEADER' THEN 0"
                                + " WHEN 'OFFICER' THEN 1 ELSE 2 END, joined_at"));
                guild.put("claims", rows(connection,
                        "SELECT id,world,x1,y1,z1,x2,y2,z2,creator_uuid,created_at"
                                + " FROM guild_claims WHERE guild_id=" + id + " ORDER BY id"));
                guild.put("xpSources", rows(connection,
                        "SELECT source,SUM(amount) amount FROM guild_xp_ledger"
                                + " WHERE guild_id=" + id
                                + " GROUP BY source ORDER BY amount DESC"));
            }
            snapshot.put("guilds", guilds);
            snapshot.put("seasons", rows(connection,
                    "SELECT id,name,starts_at,ends_at,status FROM guild_seasons"
                            + " ORDER BY id DESC LIMIT 20"));
        }
        return snapshot;
    }

    Map<String, Object> questSnapshot(File database) throws Exception {
        Map<String, Object> snapshot = availability(database);
        if (!database.isFile()) {
            return snapshot;
        }
        try (Connection connection = openReadOnly(database)) {
            List<Map<String, Object>> quests = rows(connection, QUESTS_SQL);
            for (Map<String, Object> quest : quests) {
                Object id = quest.get("id");
                long questId = id instanceof Number
                        ? ((Number) id).longValue()
                        : Long.parseLong(String.valueOf(id));
                quest.put("participants", rows(connection,
                        "SELECT player,contribution FROM quest_participants WHERE quest_id="
                                + questId + " ORDER BY contribution DESC"));
            }
            snapshot.put("quests", quests);
        }
        return snapshot;
    }

    private static Map<String, Object> availability(File database) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("available", database.isFile());
        if (!database.isFile()) {
            snapshot.put("path", database.getAbsolutePath());
        }
        return snapshot;
    }

    private static Connection openReadOnly(File database) throws Exception {
        Class.forName("org.sqlite.JDBC");
        String path = database.getAbsolutePath().replace("\\", "/");
        Connection connection = DriverManager.getConnection(
                "jdbc:sqlite:file:" + path + "?mode=ro");
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA query_only=ON");
            statement.execute("PRAGMA busy_timeout=5000");
        } catch (SQLException error) {
            connection.close();
            throw error;
        }
        return connection;
    }

    private static List<Map<String, Object>> rows(Connection connection, String sql)
            throws SQLException {
        List<Map<String, Object>> output = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery(sql)) {
            ResultSetMetaData metadata = results.getMetaData();
            int count = metadata.getColumnCount();
            while (results.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int column = 1; column <= count; column++) {
                    Object value = results.getObject(column);
                    if (value instanceof byte[] bytes) {
                        value = Base64.getEncoder().encodeToString(bytes);
                    }
                    row.put(metadata.getColumnLabel(column), value);
                }
                output.add(row);
            }
        }
        return output;
    }

    private static String sqlQuote(String value) {
        return "'" + value.replace("'", "''") + "'";
    }
}
