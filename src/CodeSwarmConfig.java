/*
    Copyright 2008 Michael Ogawa

    This file is part of code_swarm.

    code_swarm is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    code_swarm is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with code_swarm.  If not, see <http://www.gnu.org/licenses/>.
*/

import java.io.*;
import java.util.Properties;

public class CodeSwarmConfig
{
    public static String WIDTH_KEY = "Width";
    public static String HEIGHT_KEY = "Height";
    public static String INPUT_FILE_KEY = "InputFile";
    public static String MSEC_PER_FRAME_KEY = "MillisecondsPerFrame";
    public static String TAKE_SNAPSHOTS_KEY = "TakeSnapshots";

    private Properties p = null;

    public CodeSwarmConfig(String configFileName) throws IOException
    {
        p = new Properties();
        p.setProperty(WIDTH_KEY, "640");
        p.setProperty(HEIGHT_KEY, "480");
        p.setProperty(INPUT_FILE_KEY, "data/sample-repevents.xml");
        p.setProperty(MSEC_PER_FRAME_KEY, "21600000");
        p.setProperty(TAKE_SNAPSHOTS_KEY, "false");
        p.load(new FileInputStream(configFileName));
    }

    public int getWidth()
    {
        return Integer.valueOf(p.getProperty(WIDTH_KEY));
    }

    public int getHeight()
    {
        return Integer.valueOf(p.getProperty(HEIGHT_KEY));
    }

    public String getInputFile()
    {
        return p.getProperty(INPUT_FILE_KEY);
    }

    public int getMSecPerFrame()
    {
        return Integer.valueOf(p.getProperty(MSEC_PER_FRAME_KEY));
    }

    public boolean getTakeSnapshots()
    {
        return Boolean.valueOf(p.getProperty(TAKE_SNAPSHOTS_KEY));
    }

    public static void main(String[] args)
    {
        if (args.length > 0)
        {
            CodeSwarmConfig config = null;
            try
            {
                config = new CodeSwarmConfig(args[0]);
                System.out.println(CodeSwarmConfig.WIDTH_KEY + " = " + config.getWidth());
                System.out.println(CodeSwarmConfig.HEIGHT_KEY + " = " + config.getHeight());
                System.out.println(CodeSwarmConfig.INPUT_FILE_KEY + " = " + config.getInputFile());
                System.out.println(CodeSwarmConfig.MSEC_PER_FRAME_KEY + " = " + config.getMSecPerFrame());
                System.out.println(CodeSwarmConfig.TAKE_SNAPSHOTS_KEY + " = " + config.getTakeSnapshots());
            }
            catch (IOException e)
            {
                System.err.println("Failed due to exception: " + e.getMessage());
            }
        }
        else
        {
            System.err.println("Requires config file.");
        }
    }
}
