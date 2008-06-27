package codeswarm.jogl;

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

import java.util.logging.Logger;

/**
 * This is the Java OpenGL version of the original processing code_swarm application
 *
 * It is modelled after the original prototype.
 *
 * @author <a href="mailto:arjenw@gmail.com">Arjen Wiersma</a>
 */
public class CodeSwarm {
    public static final Logger log = Logger.getLogger(CodeSwarm.class.getName());

    public static void main(String[] args) {
        log.info("Running CodeSwarm with parameters: ");
        for (String arg : args) {
            log.info("Argument: " + arg);
        }
    }
}
