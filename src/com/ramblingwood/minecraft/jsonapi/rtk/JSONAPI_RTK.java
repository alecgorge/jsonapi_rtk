package com.ramblingwood.minecraft.jsonapi.rtk;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.util.config.Configuration;

import com.drdanick.McRKit.ToolkitEvent;
import com.drdanick.McRKit.module.Module;
import com.drdanick.McRKit.module.ModuleLoader;
import com.drdanick.McRKit.module.ModuleMetadata;
import com.ramblingwood.minecraft.jsonapi.rtk.RTKToolkit.RTKInterface;
import com.ramblingwood.minecraft.jsonapi.rtk.RTKToolkit.RTKInterfaceException;
import com.ramblingwood.minecraft.jsonapi.rtk.RTKToolkit.RTKListener;

public class JSONAPI_RTK extends Module implements RTKListener {
	private Configuration config = new Configuration(new File("plugins"+File.separator+"JSONAPI", "config_rtk.yml"));
	private JSONServer server;
	public RTKInterface api;
	
	public String salt = "";
	public int port = 20059;
	public List<String> whitelist = new ArrayList<String>();
	public HashMap<String, String> auth = new HashMap<String, String>();

	public JSONAPI_RTK(ModuleMetadata meta, ModuleLoader moduleLoader, ClassLoader cLoader) {
		super(meta, moduleLoader, cLoader, ToolkitEvent.ON_SERVER_HOLD, ToolkitEvent.ON_SERVER_RESTART);
		
		config.load();
		try {
			api = RTKInterface.createRTKInterface(config.getInt("RTK.port", 25561), "localhost", config.getString("RTK.username", "user"), config.getString("RTK.password", "pass"));
		} catch (RTKInterfaceException e) {
			e.printStackTrace();
		}
		config.save();
	}
	
	private void loadJSONAPIConfig() {
		String s = File.separator;
		Configuration yamlConfig = new Configuration(new File("plugins"+s+"JSONAPI", "config.yml"));
		yamlConfig.load(); // VERY IMPORTANT
		
		whitelist = yamlConfig.getStringList("options.ip-whitelist", new ArrayList<String>());						
		salt = yamlConfig.getString("options.salt", "");
		port = yamlConfig.getInt("options.port", 20059);
		
		List<String> logins = yamlConfig.getKeys("logins");
		for(String k : logins) {
			auth.put(k, yamlConfig.getString("logins."+k));
		}
	}

	protected void onDisable() {
		try {
			server.stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	protected void onEnable() {
		loadJSONAPIConfig();
		
		try {
			api.registerRTKListener(this);
			server = new JSONServer(auth, this);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void onRTKStringReceived(String paramString) {
		System.out.println("[JSONAPI_RTK] From wrapper: " + paramString);
	}
}