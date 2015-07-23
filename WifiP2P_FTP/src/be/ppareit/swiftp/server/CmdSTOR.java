/*
Copyright 2009 David Revell

This file is part of SwiFTP.

SwiFTP is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

SwiFTP is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with SwiFTP.  If not, see <http://www.gnu.org/licenses/>.
 */

package be.ppareit.swiftp.server;


/**
 * STOR - server-DTP receives file and stores it on the server. Contents will be overwriteen
 * 
 * @author David Revell, Pieter Pareit
 */

public class CmdSTOR extends CmdAbstractStore implements Runnable {
    protected String input;
    @SuppressWarnings("unused")
	private static String TAG = CmdSTOR.class.toString();

    public CmdSTOR(SessionThread sessionThread, String input, StatusListener status_listener) {
        super(sessionThread, CmdSTOR.class.toString(), status_listener);
        this.input = input;
    }

    @Override
    public void run() {
        doStorOrAppe(getParameter(input, true), false);
    }
}
