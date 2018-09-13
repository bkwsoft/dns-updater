package net.wachsmuths.dns;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import net.wachsmuths.dns.DynamicDnsProperties.Host;

@Service
public class DynamicDnsDaemon {
	private static final Logger LOG = LoggerFactory.getLogger(DynamicDnsDaemon.class);
	
	@Autowired
	private DynamicDnsProperties dnsProperties;
	
	private Map<String, String> addressCache = new HashMap<>();
	
	@Scheduled(fixedDelay=60000)
	public void process() {
		LOG.info("Checking for IP updates:");
		for (Host host : dnsProperties.getHosts().values()) {
			LOG.info("\tProcessing " + host.getHostName());
			String address = lookupIP(host.getInterfaceName());
			
			if (address != null) {
				LOG.info("\t\t" + address);
				isNewIP(host, address);
			}
		}
	}
	
	private String lookupIP(String networkInterface) {
		try {
			NetworkInterface iface = NetworkInterface.getByName(networkInterface);
			
			if (iface == null) {
				LOG.error("Interface " + networkInterface + " not found!");
				return null;
			}
			
			for (Enumeration<InetAddress> enumIpAddr = iface.getInetAddresses(); enumIpAddr.hasMoreElements();) {
				 InetAddress address = enumIpAddr.nextElement();
				 
				 if (address instanceof Inet6Address && ((Inet6Address) address).getScopeId() == 0) {
					 return address.getHostAddress().split("%")[0];
				 }
			}

			return null;
		} catch (SocketException e) {
			LOG.error("Failed to enumerate network interface.", e);
			return null;
		}
		
	}
	
	private boolean isNewIP(Host host, String address) {
		boolean result = false;
		
		if (addressCache.containsKey(host.getHostName())) {
			if (address.equals(addressCache.get(host.getHostName()))) {
				LOG.info("No change detected in IP address.");
			} else {
				result = true;
			}
		} else {
			String hostAddress = lookupHostIP(host);
			addressCache.put(host.getHostName(), hostAddress);
			if (address.equals(hostAddress)) {
				LOG.info("No change detected in IP address.");
			} else {
				result = true;
			}
		}
		
		return result;
	}
	
	private String lookupHostIP(Host host) {
		try {
			InetAddress[] allAddresses = InetAddress.getAllByName(host.getHostName() + "." + dnsProperties.getDomain());
			
			for (InetAddress hostAddress : allAddresses) {
				if (hostAddress instanceof Inet6Address && ((Inet6Address) hostAddress).getScopedInterface().getName().equals(host.getInterfaceName())) {
					return hostAddress.getHostAddress().split("%")[0];
				}
			}
			
		} catch (UnknownHostException e) {
			LOG.error("Can't find IP address for host!", e);
		}

		return null;
	}
}
