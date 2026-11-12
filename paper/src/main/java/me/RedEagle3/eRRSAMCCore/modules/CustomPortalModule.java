package me.RedEagle3.eRRSAMCCore.modules;

import me.RedEagle3.eRRSAMCCore.ERRSAMCCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

/** Owns custom-portals configuration and the portal subsystem lifecycle. */
public final class CustomPortalModule extends CoreModule {

    private PortalPlayerListener listener;
    private PortalAdminCommand adminCommand;

    public CustomPortalModule(ERRSAMCCore plugin) {
        super(plugin, "custom-portals");
    }

    @Override
    public void onEnable() {
        listener = new PortalPlayerListener(this);
        adminCommand = new PortalAdminCommand(this, listener);
        listener.start();
        getLogger().info("Custom portals enabled (custom-portals/config.yml).");
    }

    @Override
    public void onDisable() {
        if (listener != null) {
            listener.stop();
            listener = null;
        }
        adminCommand = null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        return adminCommand != null
                && adminCommand.onCommand(sender, command, label, args);
    }
}
