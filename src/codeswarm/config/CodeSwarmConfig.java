package codeswarm.config;

/*
    Copyright 2008 Michael Ogawa
	Additional work by:
	   Arjen Wiersma
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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class CodeSwarmConfig {
    public final static String WIDTH_KEY = "Width";
    public final static String HEIGHT_KEY = "Height";
    public final static String INPUT_FILE_KEY = "InputFile";
    public final static String MSEC_PER_FRAME_KEY = "MillisecondsPerFrame";
    public final static String TAKE_SNAPSHOTS_KEY = "TakeSnapshots";

    private Properties p = null;

    public CodeSwarmConfig(String configFileName) throws IOException {
        p = new Properties();
        p.setProperty(WIDTH_KEY, "640");
        p.setProperty(HEIGHT_KEY, "480");
        p.setProperty(INPUT_FILE_KEY, "data/sample-repevents.xml");
        p.setProperty(MSEC_PER_FRAME_KEY, "21600000");
        p.setProperty(TAKE_SNAPSHOTS_KEY, "false");
        p.load(new FileInputStream(configFileName));
    }

    public boolean getBooleanProperty(String key, boolean defValue) {
        return Boolean.valueOf(p.getProperty(key, String.valueOf(defValue)));
    }

    public String getStringProperty(String key, String defValue) {
        return p.getProperty(key, defValue);
    }

    public int getIntProperty(String key, int defValue) {
        return Integer.valueOf(p.getProperty(key, String.valueOf(defValue)));
    }
}