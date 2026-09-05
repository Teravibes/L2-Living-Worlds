package org.l2jmobius.gameserver.util.argsparse;

/**
 * @author Heelix
 */
public final class GameServerLaunchArgumentsParser {

    private static final String CONFIG_PATH_KEY = "gameConfigPath";
    private static final String DEFAULT_CONFIG_PATH = "config";

    private GameServerLaunchArgumentsParser()
    {
    }

    public static GameServerLaunchArgs parse()
    {
        String baseGameConfigPath = System.getProperty(CONFIG_PATH_KEY, DEFAULT_CONFIG_PATH);

        return new GameServerLaunchArgs(baseGameConfigPath);
    }
}
