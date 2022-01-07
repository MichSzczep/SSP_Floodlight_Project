package pl.edu.agh.kt;

import java.util.Collection;
import java.util.Map;

import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.types.OFPort;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.packet.ARP;
import net.floodlightcontroller.packet.IPv4;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SdnLabListener implements IFloodlightModule, IOFMessageListener {

	protected IFloodlightProviderService floodlightProvider;
	protected static Logger logger;
	protected IPv4 ipv4;

	@Override
	public String getName() {
		return SdnLabListener.class.getSimpleName();
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public net.floodlightcontroller.core.IListener.Command receive(IOFSwitch sw, OFMessage msg,
			FloodlightContext cntx) {

		logger.info("************* NEW PACKET IN *************");
		PacketExtractor extractor = new PacketExtractor();
		extractor.packetExtract(cntx);
		IPv4 ipv4 = extractor.getIPv4();
		ARP arp = extractor.getARP();
		
		logger.warn("IPv4: {}", ipv4);
		logger.warn("ARP: {}", arp);

		OFPacketIn pin = (OFPacketIn) msg;
		OFPort outPort=OFPort.of(0);
		
		logger.warn("Switch: {}", sw.getId().toString());
		
		if (sw.getId().toString().matches("00:00:00:00:00:00:00:01")) {
			outPort = OFPort.of(0);
			
			if (pin.getInPort() == OFPort.of(1) || pin.getInPort() == OFPort.of(2) || pin.getInPort() == OFPort.of(3)){
				outPort=OFPort.of(4);
			} else {
				
				if (ipv4==null && arp!=null) {
					
					logger.info("Dest Protocol Address: {}, Hardware Address: {}", arp.getTargetProtocolAddress(), arp.getTargetHardwareAddress());
					if (arp.getTargetProtocolAddress().toString().matches("10.0.0.1")) {
						outPort=OFPort.of(1);
					} else if (arp.getTargetProtocolAddress().toString().matches("10.0.0.2")) {
						outPort=OFPort.of(2);
					} else if (arp.getTargetProtocolAddress().toString().matches("10.0.0.3")) {
						outPort=OFPort.of(3);
					} else {
						logger.error("Incoming ARP packet from Port 1 switch: {} has non-matching destination IP address: {}",sw.getId(), arp.getTargetProtocolAddress());
					}
					
				} else {

					if (ipv4.getDestinationAddress().toString().matches("10.0.0.1")) {
						outPort=OFPort.of(1);
					} else if (ipv4.getDestinationAddress().toString().matches("10.0.0.2")) {
						outPort=OFPort.of(2);
					} else if (ipv4.getDestinationAddress().toString().matches("10.0.0.3")) {
						outPort=OFPort.of(3);
					} else {
						logger.error("Incoming packet from Port 4 switch: {} has non-matching destination IP address: {}", sw.getId(), ipv4.getDestinationAddress());
					}
				}
				
			}	
			// ADD FLOW 
			Flows.simpleAdd(sw, outPort, pin, cntx);
			
		} else {
			
			outPort = OFPort.of(0);
			
			if (pin.getInPort() == OFPort.of(2) || pin.getInPort() == OFPort.of(3)){
				outPort=OFPort.of(1);
			} else {
				
				if (ipv4==null && arp!=null) {
					logger.info("Dest Protocol Address: {}, Hardware Address: {}", arp.getTargetProtocolAddress(), arp.getTargetHardwareAddress());
					if (arp.getTargetProtocolAddress().toString().matches("10.0.0.4")) {
						outPort=OFPort.of(2);
					} else if (arp.getTargetProtocolAddress().toString().matches("10.0.0.5")) {
						outPort=OFPort.of(3);
					} else {
						logger.error("Incoming ARP packet from Port 1 switch: {} has non-matching destination IP address: {}",sw.getId(), arp.getTargetProtocolAddress());
					}
				} else {
					logger.warn("Dest IP: {}", ipv4.getDestinationAddress());
					if (ipv4.getDestinationAddress().toString().matches("10.0.0.4")) {
						outPort=OFPort.of(2);
					} else if (ipv4.getDestinationAddress().toString().matches("10.0.0.5")) {
						outPort=OFPort.of(3);
					} else {
						logger.error("Incoming packet from Port 1 switch: {} has non-matching destination IP address: {}",sw.getId(), ipv4.getDestinationAddress());
					}
				}
				
			}
			// ADD FLOW 
			Flows.simpleAdd(sw, outPort, pin, cntx);
			
		}
		
		
		//TODO LAB 6
		
		return Command.STOP;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		logger = LoggerFactory.getLogger(SdnLabListener.class);
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		logger.info("******************* START **************************");

	}

}
