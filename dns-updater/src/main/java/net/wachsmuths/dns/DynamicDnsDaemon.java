package net.wachsmuths.dns;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

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
				if (isNewIP(host, address)) {
					updateDNS(host.getHostName(), address);
				}
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
				 
				 LOG.info("Checking IP for " + address.getHostAddress());
				 if (address instanceof Inet6Address && !address.isLinkLocalAddress()) {
					 LOG.info("Found Global IPV6 IP!!!");
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
				if (hostAddress instanceof Inet6Address) {
					return hostAddress.getHostAddress().split("%")[0];
				}
			}
			
		} catch (UnknownHostException e) {
			LOG.error("Can't find IP address for host!", e);
		}

		return null;
	}
	
	protected String hash(String password) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("MD5");
		md.update(password.getBytes());
		byte[] digest = md.digest();
		StringBuffer sb = new StringBuffer();
		
		for (byte b : digest) {
			sb.append(String.format("%02x", b & 0xff));
		}
		
		return sb.toString();
	}
	
	private void updateDNS(String hostName, String address) {
		LOG.info("Updating DNS alias for host: " + hostName + " to IPV6: " + address);
		
		RestTemplate template = new RestTemplate();
		
		URI url;
		try {
			url = UriComponentsBuilder.fromUriString("https://api.dynu.com/nic/update")
					.queryParam("hostname", dnsProperties.getDomain())
					.queryParam("alias", hostName)
					.queryParam("myipv6", address)
					.queryParam("username", dnsProperties.getUserId())
					.queryParam("password", hash(dnsProperties.getPassword()))
					.build().toUri();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Error creating DNS Update URI");
		}
		
		LOG.error("Calling: " + url.toString());
		//template.getForEntity(url, String.class);
	}
}
