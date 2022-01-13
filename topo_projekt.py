from mininet.topo import Topo

class MyTopo( Topo ):
    def __init__( self ):
    # Initialize topology
        Topo.__init__( self )
        # Add hosts and switches
        leftHost1 = self.addHost( 'client1', ip='10.0.0.1', mac='00:00:00:00:00:01')
        leftHost2 = self.addHost( 'client2', ip='10.0.0.2', mac='00:00:00:00:00:02')
        leftHost3 = self.addHost( 'client3', ip='10.0.0.3', mac='00:00:00:00:00:03' )
        leftSwitch = self.addSwitch( 'switch1' )
        rightSwitch = self.addSwitch( 'switch2' )
        rightHost1 = self.addHost( 'server1', ip='10.0.0.5', mac='00:00:00:00:00:05' )
        rightHost2 = self.addHost( 'server2', ip='10.0.0.6', mac='00:00:00:00:00:06' )
        rightHost3 = self.addHost( 'proxyArp', ip='10.0.0.4', mac='00:00:00:00:00:04' )

        # Add links
        self.addLink( leftHost1, leftSwitch )
        self.addLink( leftHost2, leftSwitch )
        self.addLink( leftHost3, leftSwitch )
        self.addLink( leftSwitch, rightSwitch )
        self.addLink( rightSwitch, rightHost1 )
        self.addLink( rightSwitch, rightHost2 )
        self.addLink( rightSwitch, rightHost3 )

topos = { 'mytopo': ( lambda: MyTopo() ) }
