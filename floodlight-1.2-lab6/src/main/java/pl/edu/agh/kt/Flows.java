package pl.edu.agh.kt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActionSetDlDst;
import org.projectfloodlight.openflow.protocol.action.OFActionSetDlSrc;
import org.projectfloodlight.openflow.protocol.action.OFActionSetNwDst;
import org.projectfloodlight.openflow.protocol.action.OFActionSetNwDst.Builder;
import org.projectfloodlight.openflow.protocol.action.OFActionSetNwSrc;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.ArpOpcode;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.TransportPort;
import org.projectfloodlight.openflow.types.VlanVid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.packet.ARP;
import net.floodlightcontroller.packet.Data;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.TCP;
import net.floodlightcontroller.packet.UDP;

public class Flows {

	private static final Logger logger = LoggerFactory.getLogger(Flows.class);
	
	public static short FLOWMOD_DEFAULT_IDLE_TIMEOUT = 5; // in seconds
	public static short FLOWMOD_DEFAULT_HARD_TIMEOUT = 0; // infinite
	public static short FLOWMOD_DEFAULT_PRIORITY = 100; 

	protected static boolean FLOWMOD_DEFAULT_MATCH_VLAN = true;
	protected static boolean FLOWMOD_DEFAULT_MATCH_MAC = true;
	protected static boolean FLOWMOD_DEFAULT_MATCH_IP_ADDR = true;
	protected static boolean FLOWMOD_DEFAULT_MATCH_TRANSPORT = true;
	
	static OFActionSetNwSrc.Builder ip_addr_src;
	static OFActionSetNwDst.Builder ip_addr_dst;
	static OFActionSetDlSrc.Builder mac_addr_src;
	static OFActionSetDlDst.Builder mac_addr_dst;

	public Flows() {
		logger.info("Flows() begin/end");
	}
	
	public static void sendPacketOut(IOFSwitch sw, Ethernet eth, MacAddress sw_mac, ARP arp, OFPort outport) {
		// Ethernet
		Ethernet l2 = new Ethernet();
		l2.setSourceMACAddress(MacAddress.of("16:56:01:e2:25:f5"));
		l2.setDestinationMACAddress(eth.getSourceMACAddress());
		l2.setEtherType(EthType.ARP);
		l2.setPriorityCode(eth.getPriorityCode());
		
		// IP
		ARP new_arp = new ARP();
		new_arp.setHardwareAddressLength(arp.getHardwareAddressLength());
		new_arp.setHardwareType(arp.getHardwareType());
		new_arp.setOpCode(ArpOpcode.of(arp.getOpCode().getOpcode()+1));
		//new_arp.setParent(arp.getParent());
		//new_arp.setPayload(arp.getPayload());
		new_arp.setProtocolAddressLength(arp.getProtocolAddressLength());
		new_arp.setProtocolType(arp.getProtocolType());
		new_arp.setSenderHardwareAddress(MacAddress.of("16:56:01:e2:25:f5"));
		new_arp.setSenderProtocolAddress(arp.getTargetProtocolAddress());
		new_arp.setTargetHardwareAddress(arp.getSenderHardwareAddress());
		new_arp.setTargetProtocolAddress(arp.getSenderProtocolAddress());

		// set the payloads of each layer
		l2.setPayload(new_arp);
		l2.resetChecksum();
		
		// serialize
		byte[] serializedData = l2.serialize();

		// Create Packet-Out and Write to Switch
		List<OFAction> actions = new ArrayList<OFAction>();
		actions.add(sw.getOFFactory().actions().buildOutput().setPort(outport).setMaxLen(0xffFFffFF).build());
		
		OFPacketOut po = sw.getOFFactory()
		.buildPacketOut()
		.setData(serializedData)
		.setActions(actions)
		.setInPort(OFPort.CONTROLLER).build();
		sw.write(po);

	}

	public static void forwardFirstPacket(IOFSwitch sw, OFPacketIn pi, OFPort outport) {

		OFPort inPort = (pi.getVersion().compareTo(OFVersion.OF_12) < 0 ? pi.getInPort()
				: pi.getMatch().get(MatchField.IN_PORT));
		OFPacketOut.Builder pob = sw.getOFFactory().buildPacketOut();
		List<OFAction> actions = new ArrayList<OFAction>();
		actions.add(sw.getOFFactory().actions().buildOutput().setPort(outport).setMaxLen(0xffFFffFF).build());
		pob.setActions(actions);

		if (sw.getBuffers() == 0) {
			pi = pi.createBuilder().setBufferId(OFBufferId.NO_BUFFER).build();
			pob.setBufferId(OFBufferId.NO_BUFFER);
			logger.info("The switch doesn't support buffering");
		} else {
			pob.setBufferId(pi.getBufferId());
		}
		
		if (pi.getBufferId() == OFBufferId.NO_BUFFER) {
			byte[] packetData = pi.getData();
			pob.setData(packetData);
		}
		
		sw.write(pob.build());

	}
	
	
//	public static void simpleAdd(IOFSwitch sw, OFPort outPort, OFPacketIn pin) {
//		// FlowModBuilder
//		OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowAdd();
//
//		// match
//		Match.Builder mb = sw.getOFFactory().buildMatch();
//		mb.setExact(MatchField.IN_PORT, outPort);
//		Match m = mb.build();
//
//		// actions
//		OFActionOutput.Builder aob = sw.getOFFactory().actions().buildOutput();
//		aob.setPort(pin.getInPort());
//		aob.setMaxLen(Integer.MAX_VALUE);
//		List<OFAction> actions = new ArrayList<OFAction>();
//		actions.add(aob.build());
//		
//		
//		fmb.setMatch(m)
//		.setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT)
//		.setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
//		.setBufferId(pin.getBufferId())
//		.setOutPort(outPort)
//		.setPriority(FLOWMOD_DEFAULT_PRIORITY);
//		fmb.setActions(actions);
//		
//		// write flow to switch
//		try {
//			sw.write(fmb.build());
//			logger.info("Flow from port {} forwarded to port {}; match: {}", new Object[] { pin.getInPort().getPortNumber(), outPort.getPortNumber(), m.toString() });
//		} catch (Exception e) {
//			logger.error("error {}", e);
//		}
//		
//	}

	public static void simpleAdd(IOFSwitch sw, OFPort outPort, OFPacketIn pin,FloodlightContext cntx) {
		// FlowModBuilder
		OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowAdd();

		// match
		//Match.Builder mb = sw.getOFFactory().buildMatch();
		//mb.setExact(MatchField.IN_PORT, pin.getInPort());
		Match m = createMatchFromPacket(sw, pin.getInPort(), cntx);

		// actions
		OFActionOutput.Builder aob = sw.getOFFactory().actions().buildOutput();
		aob.setPort(outPort);
		aob.setMaxLen(Integer.MAX_VALUE);
		List<OFAction> actions = new ArrayList<OFAction>();
		actions.add(aob.build());
		
		
		fmb.setMatch(m)
		.setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT)
		.setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
		.setBufferId(pin.getBufferId())
		.setOutPort(outPort)
		.setPriority(FLOWMOD_DEFAULT_PRIORITY);
		fmb.setActions(actions);
		
		// write flow to switch
		try {
			sw.write(fmb.build());
			logger.info("Flow from port {} forwarded to port {}; match: {}", new Object[] { pin.getInPort().getPortNumber(), outPort.getPortNumber(), m.toString() });
		} catch (Exception e) {
			logger.error("error {}", e);
		}
		
	}

	//TODO punkt 3 instrukcji do laboratorium 6
	public static void simpleAdd(IOFSwitch sw, OFPort outPort, OFPacketIn pin,FloodlightContext cntx, IPv4Address ipv4, int direction) {
		// FlowModBuilder
		OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowAdd();

		// match
		//Match.Builder mb = sw.getOFFactory().buildMatch();
		//mb.setExact(MatchField.IN_PORT, pin.getInPort());
		Match m = createMatchFromPacket(sw, pin.getInPort(), cntx);

		// actions
		if (direction==1) {
			ip_addr_src = sw.getOFFactory().actions().buildSetNwSrc();
			ip_addr_src.setNwAddr(ipv4);
			OFActionOutput.Builder aob = sw.getOFFactory().actions().buildOutput();
			aob.setPort(outPort);
			aob.setMaxLen(Integer.MAX_VALUE);
			List<OFAction> actions = new ArrayList<OFAction>();
			actions.add(ip_addr_src.build());
			actions.add(aob.build());
			
			
			fmb.setMatch(m)
			.setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT)
			.setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
			.setBufferId(pin.getBufferId())
			.setOutPort(outPort)
			.setPriority(FLOWMOD_DEFAULT_PRIORITY);
			fmb.setActions(actions);
			
			// write flow to switch
			try {
				sw.write(fmb.build());
				logger.info("Flow from port {} forwarded to port {}; match: {}", new Object[] { pin.getInPort().getPortNumber(), outPort.getPortNumber(), m.toString() });
			} catch (Exception e) {
				logger.error("error {}", e);
			}
			
		} else {
			
			ip_addr_dst = sw.getOFFactory().actions().buildSetNwDst();
			ip_addr_dst.setNwAddr(ipv4);
			OFActionOutput.Builder aob = sw.getOFFactory().actions().buildOutput();
			aob.setPort(outPort);
			aob.setMaxLen(Integer.MAX_VALUE);
			List<OFAction> actions = new ArrayList<OFAction>();
			actions.add(ip_addr_dst.build());
			actions.add(aob.build());
			
			
			fmb.setMatch(m)
			.setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT)
			.setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
			.setBufferId(pin.getBufferId())
			.setOutPort(outPort)
			.setPriority(FLOWMOD_DEFAULT_PRIORITY);
			fmb.setActions(actions);
			
			// write flow to switch
			try {
				sw.write(fmb.build());
				logger.info("Flow from port {} forwarded to port {}; match: {}", new Object[] { pin.getInPort().getPortNumber(), outPort.getPortNumber(), m.toString() });
			} catch (Exception e) {
				logger.error("error {}", e);
			}
		}
		
	}
	
	public static void simpleAdd(IOFSwitch sw, OFPort outPort, OFPacketIn pin,FloodlightContext cntx, IPv4Address ipv4, MacAddress mac, int direction) {
		// FlowModBuilder
		OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowAdd();

		// match
		//Match.Builder mb = sw.getOFFactory().buildMatch();
		//mb.setExact(MatchField.IN_PORT, pin.getInPort());
		Match m = createMatchFromPacket(sw, pin.getInPort(), cntx);

		// actions
		if (direction==1) {
			ip_addr_src = sw.getOFFactory().actions().buildSetNwSrc();
			mac_addr_src = sw.getOFFactory().actions().buildSetDlSrc();
			
			ip_addr_src.setNwAddr(ipv4);
			mac_addr_src.setDlAddr(mac);
			OFActionOutput.Builder aob = sw.getOFFactory().actions().buildOutput();
			aob.setPort(outPort);
			aob.setMaxLen(Integer.MAX_VALUE);
			List<OFAction> actions = new ArrayList<OFAction>();
			actions.add(ip_addr_src.build());
			actions.add(mac_addr_src.build());
			actions.add(aob.build());
			
			
			fmb.setMatch(m)
			.setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT)
			.setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
			.setBufferId(pin.getBufferId())
			.setOutPort(outPort)
			.setPriority(FLOWMOD_DEFAULT_PRIORITY);
			fmb.setActions(actions);
			
			// write flow to switch
			try {
				sw.write(fmb.build());
				logger.info("Flow from port {} forwarded to port {}; match: {}", new Object[] { pin.getInPort().getPortNumber(), outPort.getPortNumber(), m.toString() });
			} catch (Exception e) {
				logger.error("error {}", e);
			}
			
		} else {
			ip_addr_dst = sw.getOFFactory().actions().buildSetNwDst();
			mac_addr_dst = sw.getOFFactory().actions().buildSetDlDst();
			
			ip_addr_dst.setNwAddr(ipv4);
			mac_addr_dst.setDlAddr(mac);
			OFActionOutput.Builder aob = sw.getOFFactory().actions().buildOutput();
			aob.setPort(outPort);
			aob.setMaxLen(Integer.MAX_VALUE);
			List<OFAction> actions = new ArrayList<OFAction>();
			actions.add(ip_addr_dst.build());
			actions.add(mac_addr_dst.build());
			actions.add(aob.build());
			
			
			fmb.setMatch(m)
			.setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT)
			.setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
			.setBufferId(pin.getBufferId())
			.setOutPort(outPort)
			.setPriority(FLOWMOD_DEFAULT_PRIORITY);
			fmb.setActions(actions);
			
			// write flow to switch
			try {
				sw.write(fmb.build());
				logger.info("Flow from port {} forwarded to port {}; match: {}", new Object[] { pin.getInPort().getPortNumber(), outPort.getPortNumber(), m.toString() });
			} catch (Exception e) {
				logger.error("error {}", e);
			}
		}
		
	}
	
	public static void simpleAdd(IOFSwitch sw, OFPort outPort, OFPacketIn pin,FloodlightContext cntx, MacAddress mac, ARP arp) {
		// FlowModBuilder
		OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowAdd();

		// match
		//Match.Builder mb = sw.getOFFactory().buildMatch();
		//mb.setExact(MatchField.IN_PORT, pin.getInPort());
		Match m = createMatchFromPacket(sw, pin.getInPort(), cntx);

		// actions
		OFActionSetDlDst.Builder dstMac = sw.getOFFactory().actions().buildSetDlDst();
		OFActionSetDlSrc.Builder srcMac = sw.getOFFactory().actions().buildSetDlSrc();
		dstMac.setDlAddr(arp.getSenderHardwareAddress());
		srcMac.setDlAddr(mac);
		OFActionOutput.Builder aob = sw.getOFFactory().actions().buildOutput();
		aob.setPort(outPort);
		aob.setMaxLen(Integer.MAX_VALUE);
		List<OFAction> actions = new ArrayList<OFAction>();
		actions.add(dstMac.build());
		actions.add(srcMac.build());
		actions.add(aob.build());
		
		
		fmb.setMatch(m)
		.setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT)
		.setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
		.setBufferId(pin.getBufferId())
		.setOutPort(outPort)
		.setPriority(FLOWMOD_DEFAULT_PRIORITY);
		fmb.setActions(actions);
		
		// write flow to switch
		try {
			sw.write(fmb.build());
			logger.info("Flow from port {} forwarded to port {}; match: {}", new Object[] { pin.getInPort().getPortNumber(), outPort.getPortNumber(), m.toString() });
		} catch (Exception e) {
			logger.error("error {}", e);
		}
		
	}


	public static Match createMatchFromPacket(IOFSwitch sw, OFPort inPort, FloodlightContext cntx) {
		// The packet in match will only contain the port number.
		// We need to add in specifics for the hosts we're routing between.
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		VlanVid vlan = VlanVid.ofVlan(eth.getVlanID());
		MacAddress srcMac = eth.getSourceMACAddress();
		MacAddress dstMac = eth.getDestinationMACAddress();

		Match.Builder mb = sw.getOFFactory().buildMatch();
		mb.setExact(MatchField.IN_PORT, inPort);

		if (FLOWMOD_DEFAULT_MATCH_MAC) {
			mb.setExact(MatchField.ETH_SRC, srcMac).setExact(MatchField.ETH_DST, dstMac);
		}

		if (FLOWMOD_DEFAULT_MATCH_VLAN) {
			if (!vlan.equals(VlanVid.ZERO)) {
				mb.setExact(MatchField.VLAN_VID, OFVlanVidMatch.ofVlanVid(vlan));
			}
		}

		// TODO Detect switch type and match to create hardware-implemented flow
		if (eth.getEtherType() == EthType.IPv4) { /*
													 * shallow check for
													 * equality is okay for
													 * EthType
													 */
			IPv4 ip = (IPv4) eth.getPayload();
			IPv4Address srcIp = ip.getSourceAddress();
			IPv4Address dstIp = ip.getDestinationAddress();

			if (FLOWMOD_DEFAULT_MATCH_IP_ADDR) {
				mb.setExact(MatchField.ETH_TYPE, EthType.IPv4).setExact(MatchField.IPV4_SRC, srcIp)
						.setExact(MatchField.IPV4_DST, dstIp);
			}

			if (FLOWMOD_DEFAULT_MATCH_TRANSPORT) {
				/*
				 * Take care of the ethertype if not included earlier, since
				 * it's a prerequisite for transport ports.
				 */
				if (!FLOWMOD_DEFAULT_MATCH_IP_ADDR) {
					mb.setExact(MatchField.ETH_TYPE, EthType.IPv4);
				}

				if (ip.getProtocol().equals(IpProtocol.TCP)) {
					TCP tcp = (TCP) ip.getPayload();
					mb.setExact(MatchField.IP_PROTO, IpProtocol.TCP).setExact(MatchField.TCP_SRC, tcp.getSourcePort())
							.setExact(MatchField.TCP_DST, tcp.getDestinationPort());
				} else if (ip.getProtocol().equals(IpProtocol.UDP)) {
					UDP udp = (UDP) ip.getPayload();
					mb.setExact(MatchField.IP_PROTO, IpProtocol.UDP).setExact(MatchField.UDP_SRC, udp.getSourcePort())
							.setExact(MatchField.UDP_DST, udp.getDestinationPort());
				}
			}
		} else if (eth.getEtherType() == EthType.ARP) { /*
														 * shallow check for
														 * equality is okay for
														 * EthType
														 */
			mb.setExact(MatchField.ETH_TYPE, EthType.ARP);
		}

		return mb.build();
	}
}
