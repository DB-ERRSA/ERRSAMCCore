package edu.errsa.wikisync;

import java.time.Instant;
import java.util.*;
import java.util.logging.Level;

import me.RedEagle3.eRRSAMCCore.ERRSAMCCore;
import me.RedEagle3.eRRSAMCCore.modules.CoreModule;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;

/** ERRSAWikiSync 1.1.0 adapted for Core. Collects Bukkit state on main thread;
 * publishes a single snapshot asynchronously 40 ticks after server startup. */
public final class WikiSyncModule extends CoreModule {
    public WikiSyncModule(ERRSAMCCore plugin) {
        super(plugin, "wiki-sync");
    }
    @Override public void onEnable() {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!plugin.isEnabled()) {
                return;
            }
            try {
                String inventory = snapshot();
                String owner = getConfig().getString("github.owner", "");
                String repo = getConfig().getString("github.repo", "");
                String branch = getConfig().getString("github.branch", "main");
                String token = getConfig().getString("github.token", "");
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        GitHubSnapshot.publish(owner, repo, branch, token, "survival", inventory, getLogger());
                    }
                    catch (Exception ex) {
                        getLogger().log(Level.SEVERE, "WikiSync publication failed", ex);
                    }
                });
            } catch (Exception | LinkageError ex) {
                getLogger().log(Level.SEVERE, "WikiSync snapshot failed", ex);
            }
        }, 40L);
    }
    private String snapshot() {
        List<Plugin> plugins = new ArrayList<>(Arrays.asList(Bukkit.getPluginManager().getPlugins()));
        plugins.sort(Comparator.comparing(Plugin::getName, String.CASE_INSENSITIVE_ORDER));
        List<String> pluginObjects = new ArrayList<>();
        for (Plugin candidate : plugins) {
            Map<String,String> fields = new LinkedHashMap<>();
            fields.put("name", candidate.getName());
            fields.put("version", candidate.getPluginMeta().getVersion());
            fields.put("description", candidate.getPluginMeta().getDescription());
            fields.put("enabled", Boolean.toString(candidate.isEnabled()));
            PluginDescriptionFile pdf = candidate.getDescription();
            List<String> perms = new ArrayList<>();
            pdf.getPermissions().stream().sorted(Comparator.comparing(Permission::getName)).forEach(p ->
                perms.add(permissionJson(p)));
            List<String> cmds = commands(pdf);
            String prefix = GitHubSnapshot.fields(fields);
            pluginObjects.add(prefix.substring(0, prefix.length() - 1) + ",\"permissions\":" +
                GitHubSnapshot.array(perms) + ",\"commands\":" + GitHubSnapshot.array(cmds) + "}");
        }
        List<String> allPermissions = new ArrayList<>();
        Bukkit.getPluginManager().getPermissions().stream().sorted(Comparator.comparing(Permission::getName))
                .forEach(p -> allPermissions.add(permissionJson(p)));
        List<String> groups = new ArrayList<>();
        if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
            LuckPerms lp = Bukkit.getServicesManager().load(LuckPerms.class);
            if (lp != null) {
                List<Group> loaded = new ArrayList<>(lp.getGroupManager().getLoadedGroups());
                loaded.sort(Comparator.comparing(Group::getName, String.CASE_INSENSITIVE_ORDER));
                for (Group group : loaded) {
                    List<Node> nodes = new ArrayList<>(group.getNodes());
                    nodes.sort(Comparator.comparing(Node::getKey, String.CASE_INSENSITIVE_ORDER));
                    List<String> members = new ArrayList<>();
                    for (Node node : nodes) {
                        Map<String,String> map = new LinkedHashMap<>();
                        map.put("node", node.getKey());
                        map.put("value", Boolean.toString(node.getValue()));
                        map.put("context", node.getContexts().toSet().toString());
                        map.put("type", node instanceof InheritanceNode ? "inheritance" : "permission");
                        members.add(GitHubSnapshot.fields(map));
                    }
                    groups.add("{\"name\":" + GitHubSnapshot.json(group.getName()) + ",\"nodes\":" +
                        GitHubSnapshot.array(members) + "}");
                }
            } else {
                getLogger().warning("LuckPerms API unavailable; publishing plugin inventory without group " +
                    "assignments.");
            }
        } else getLogger().warning("LuckPerms API unavailable; publishing plugin inventory without group assignments.");
        return "{\"schema\":2,\"server\":\"survival\",\"serverVersion\":" + GitHubSnapshot.json(Bukkit.getVersion())
                + ",\"minecraftVersion\":" + GitHubSnapshot.json(Bukkit.getMinecraftVersion())
                + ",\"capturedAt\":" + GitHubSnapshot.json(Instant.now().toString())
                + ",\"plugins\":" + GitHubSnapshot.array(pluginObjects)
                + ",\"registeredPermissions\":" + GitHubSnapshot.array(allPermissions)
                + ",\"luckPermsGroups\":" + GitHubSnapshot.array(groups) + "}";
    }
    private static String permissionJson(Permission permission) {
        Map<String,String> fields = new LinkedHashMap<>();
        fields.put("node", permission.getName());
        fields.put("description", permission.getDescription());
        fields.put("default", permission.getDefault().toString());
        return GitHubSnapshot.fields(fields);
    }
    private static List<String> commands(PluginDescriptionFile desc) {
        List<String> output = new ArrayList<>();
        Map<String, Map<String,Object>> cmds = desc.getCommands();
        if (cmds == null || cmds.isEmpty()) {
            return output;
        }
        cmds.entrySet().stream().sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER)).forEach(e -> {
            Map<String,Object> c = e.getValue() == null ? Collections.emptyMap() : e.getValue();
            String aliasJson;
            Object raw = c.get("aliases");
            List<String> aliases = new ArrayList<>();
            if (raw instanceof Collection<?> list) {
                for (Object item : list) if (item != null && !item.toString().isBlank())
                    aliases.add(item.toString().trim());
            } else if (raw != null) {
                for (String alias : raw.toString().replace("[", "").replace("]", "").split(",")) if
                    (!alias.isBlank()) aliases.add(alias.trim());
            }
            List<String> quoted = new ArrayList<>();
            for (String alias : aliases) {
                quoted.add(GitHubSnapshot.json(alias));
            }
            aliasJson = GitHubSnapshot.array(quoted);
            output.add("{\"name\":" + GitHubSnapshot.json(e.getKey())
                    + ",\"description\":" + GitHubSnapshot.json(value(c.get("description")))
                    + ",\"usage\":" + GitHubSnapshot.json(value(c.get("usage")))
                    + ",\"permission\":" + GitHubSnapshot.json(value(c.get("permission")))
                    + ",\"aliases\":" + aliasJson + "}");
        });
        return output;
    }
    private static String value(Object v) {
        return v == null || v.toString().isBlank() ? null : v.toString().trim();
    }
}
