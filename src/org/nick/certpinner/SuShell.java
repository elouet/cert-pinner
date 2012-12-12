package org.nick.certpinner;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

public class SuShell {

    private static final String TAG = SuShell.class.getSimpleName();

    // WARNING: setting this to true will dump passwords to logcat
    // set to false for release
    static final boolean DEBUG = false;

    private static final String CHAINSDD_SU = "com.noshufou.android.su";
    private static final String CHAINFIRE_SU = "eu.chainfire.supersu";

    private SuShell() {
    }

    public static boolean canGainSu(Context context) {
        String suTestScript = "#!/system/bin/sh\necho ";
        String suTestScriptValid = "SuPermsOkay";

        ArrayList<String> output = run("su", suTestScript + suTestScriptValid);
        if (output.size() == 1
                && output.get(0).trim().equals(suTestScriptValid)) {
            if (DEBUG) {
                Log.d(TAG, "Superuser command auth confirmed");
            }
            return true;

        } else {
            if (DEBUG) {
                Log.d(TAG, "Superuser command auth refused");
            }
            return false;
        }
    }

    public static boolean isSuperUserInstalled(Context ctx) {
        PackageInfo pi = findPackage(ctx, CHAINSDD_SU);
        if (pi != null) {
            if (DEBUG) {
                Log.d(TAG, "Found superuser: " + pi.packageName);
            }
            return true;
        }

        pi = findPackage(ctx, CHAINFIRE_SU);
        if (pi != null) {
            if (DEBUG) {
                Log.d(TAG, "Found superuser: " + pi.packageName);
            }
            return true;
        }

        return false;
    }

    private static PackageInfo findPackage(Context ctx, String packageName) {
        try {
            return ctx.getPackageManager().getPackageInfo(packageName, 0);
        } catch (NameNotFoundException e) {
            return null;
        }
    }

    public static ArrayList<String> runWithShell(String command) {
        return run("/system/bin/sh", command);
    }

    public static ArrayList<String> runWithSu(String command) {
        return run("su", command);
    }

    public static ArrayList<String> run(String shell, String command) {
        return run(shell, new String[] { command });
    }

    public static ArrayList<String> run(String shell, ArrayList<String> commands) {
        String[] commandsArray = new String[commands.size()];
        commands.toArray(commandsArray);
        return run(shell, commandsArray);
    }

    public static ArrayList<String> run(String shell, String[] commands) {
        ArrayList<String> output = new ArrayList<String>();

        try {
            Process process = Runtime.getRuntime().exec(shell);

            BufferedOutputStream shellInput = new BufferedOutputStream(
                    process.getOutputStream());
            BufferedReader shellOutput = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            for (String command : commands) {
                if (DEBUG) {
                    Log.i(TAG, "command: " + command);
                }
                shellInput.write((command + " 2>&1\n").getBytes());
            }

            shellInput.write("exit\n".getBytes());
            shellInput.flush();

            String line;
            while ((line = shellOutput.readLine()) != null) {
                if (DEBUG) {
                    Log.d(TAG, "command output: " + line);
                }
                output.add(line);
            }

            process.waitFor();
        } catch (IOException e) {
            Log.e(TAG, "Error: " + e.getMessage(), e);
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Log.e(TAG, "Error: " + e.getMessage(), e);
            throw new RuntimeException(e);
        }

        return output;
    }

    public static final String getCommandOutput(String command)
            throws IOException {

        StringBuilder output = new StringBuilder();

        if (DEBUG) {
            Log.d(TAG, "Getting output for command: " + command);
        }
        Process p = Runtime.getRuntime().exec(command);
        InputStream is = p.getInputStream();
        InputStreamReader r = new InputStreamReader(is);
        BufferedReader in = new BufferedReader(r);

        String line;
        while ((line = in.readLine()) != null) {
            output.append(line);
            output.append("\n");
        }

        return output.toString();
    }

    public boolean detectValidSuInPath() {
        String[] pathToTest = System.getenv("PATH").split(":");

        for (String path : pathToTest) {
            File su = new File(path + "/su");
            if (su.exists()) {
                // should check if it is suid 
                if (DEBUG) {
                    Log.d(TAG, "Found su at " + su.getAbsolutePath());
                }
            }
        }

        return false;
    }

}
