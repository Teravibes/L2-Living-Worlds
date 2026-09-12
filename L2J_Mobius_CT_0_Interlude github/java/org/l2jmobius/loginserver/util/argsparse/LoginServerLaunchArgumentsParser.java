package org.l2jmobius.loginserver.util.argsparse;

/**
 * @author Heelix
 */
public final class LoginServerLaunchArgumentsParser
{

    private static final String CONFIG_PATH_KEY = "loginConfigPath";
    private static final String DEFAULT_CONFIG_PATH = "config";

    private LoginServerLaunchArgumentsParser()
    {
    }

    public static LoginServerLaunchArgs parse()
    {
        String baseLoginConfigPath = System.getProperty(CONFIG_PATH_KEY, DEFAULT_CONFIG_PATH);

        return new LoginServerLaunchArgs(baseLoginConfigPath);
    }
}
