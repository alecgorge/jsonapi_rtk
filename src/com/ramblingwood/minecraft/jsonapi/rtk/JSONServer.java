package com.ramblingwood.minecraft.jsonapi.rtk;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Properties;

import com.ramblingwood.minecraft.jsonapi.rtk.RTKToolkit.RTKInterface;


public class JSONServer extends NanoHTTPD {
	HashMap<String, String> logins = new HashMap<String, String>();

	private JSONAPI_RTK inst;
	
	public JSONServer(HashMap<String, String> logins, JSONAPI_RTK plugin) throws IOException {
		super(plugin.port);
		inst = plugin;
		
		this.logins = logins;
	}
	
	public boolean testLogin (String method, String hash) {
		try {
			boolean valid = false;
			
			for(String user : logins.keySet()) {
				String pass = logins.get(user);

				String thishash = SHA256(user+method+pass+inst.salt);
				
				if(thishash.equals(hash)) {
					valid = true;
					break;
				}
			}
			
			return valid;
		}
		catch (Exception e) {
			return false;
		}
	}

	/**
	 * From a password, a number of iterations and a salt,
	 * returns the corresponding digest
	 * @param iterationNb int The number of iterations of the algorithm
	 * @param password String The password to encrypt
	 * @param salt byte[] The salt
	 * @return byte[] The digested password
	 * @throws NoSuchAlgorithmException If the algorithm doesn't exist
	 */
	public static String SHA256(String password) throws NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		digest.reset();
		byte[] input = null;
		try {
			input = digest.digest(password.getBytes("UTF-8"));
			StringBuffer hexString = new StringBuffer();
			for(int i = 0; i< input.length; i++) {
				String hex = Integer.toHexString(0xFF & input[i]);
				if (hex.length() == 1) {
					hexString.append('0');
				}
				hexString.append(hex);
			}
			return hexString.toString();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return "UnsupportedEncodingException";
	}
	
	@Override
	public Response serve( String uri, String method, Properties header, Properties parms )	{
		String calledMethod = parms.getProperty("method");
		String key = parms.getProperty("key");
		
		if(uri.equals("/api/call") && calledMethod.equals("remotetoolkit.startServer")) {
			if(inst.whitelist.size() > 0 && !inst.whitelist.contains(header.get("X-REMOTE-ADDR"))) {
				return new NanoHTTPD.Response(NanoHTTPD.HTTP_FORBIDDEN, NanoHTTPD.MIME_JSON, String.format("{\"result\": \"error\",\"source\": \"%s\",\"error\": \"You are not allowed to make API calls.\"}", calledMethod));
			}
			if(!testLogin(calledMethod, key)) {
				return new NanoHTTPD.Response(NanoHTTPD.HTTP_FORBIDDEN, NanoHTTPD.MIME_JSON, String.format("{\"result\": \"error\",\"source\": \"%s\",\"error\": \"Invalid API key.\"}", calledMethod));
			}
			
			try {
				inst.api.executeCommand(RTKInterface.CommandType.UNHOLD_SERVER);
				return new NanoHTTPD.Response(NanoHTTPD.HTTP_OK, NanoHTTPD.MIME_JSON, String.format("{\"result\":\"success\", \"source\":\"%s\", \"success\": true}", calledMethod));
			} catch (IOException e) {
				e.printStackTrace();
				return new NanoHTTPD.Response(NanoHTTPD.HTTP_INTERNALERROR, NanoHTTPD.MIME_JSON, String.format("{\"result\": \"error\",\"source\": \"%s\",\"error\": \"IO error turning on the server!\"}", calledMethod));
			}
		}
		else {
			return new NanoHTTPD.Response(NanoHTTPD.HTTP_INTERNALERROR, NanoHTTPD.MIME_JSON, String.format("{\"result\": \"error\",\"source\": \"%s\",\"error\": \"JSONAPI and the Minecraft are currently down. Use remotetoolkit.startServer to restart the server and have access to all the API methods.\"}", calledMethod));
		}
	}

}
