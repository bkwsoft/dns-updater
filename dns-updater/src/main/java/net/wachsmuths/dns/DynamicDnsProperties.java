package net.wachsmuths.dns;

import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="dynamic-dns")
public class DynamicDnsProperties {
	private String userId;
	private String password;
	private String domain;
	private long interval;
	private Map<String, Host> hosts;
	
	public String getUserId() {
		return userId;
	}
	
	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	public String getPassword() {
		return password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public String getDomain() {
		return domain;
	}
	
	public void setDomain(String domain) {
		this.domain = domain;
	}
	
	public long getInterval() {
		return interval;
	}
	
	public void setInterval(long interval) {
		this.interval = interval;
	}
	
	public Map<String, Host> getHosts() {
		return hosts;
	}

	public void setHosts(Map<String, Host> hosts) {
		this.hosts = hosts;
	}
	
	public static class Host {
		private String hostName;
		private String interfaceName;
		
		public String getHostName() {
			return hostName;
		}
		
		public void setHostName(String hostName) {
			this.hostName = hostName;
		}
		
		public String getInterfaceName() {
			return interfaceName;
		}
		
		public void setInterfaceName(String interfaceName) {
			this.interfaceName = interfaceName;
		}

		@Override
		public String toString() {
			return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
		}
	}
}
