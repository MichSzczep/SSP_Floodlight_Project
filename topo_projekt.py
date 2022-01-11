from mininet.topo import Topo

class MyTopo( Topo ):
    def __init__( self ):
    # Initialize topology
        Topo.__init__( self )
        # Add hosts and switches
        leftHost1 = self.addHost( 'client1', mac='11:00:00:00:00:01')
        leftHost2 = self.addHost( 'client2', mac='11:00:00:00:00:02' )
        leftHost3 = self.addHost( 'client3', mac='11:00:00:00:00:03' )
        leftSwitch = self.addSwitch( 'switch1' )
        rightSwitch = self.addSwitch( 'switch2' )
        rightHost1 = self.addHost( 'server1', mac='99:00:00:00:00:01' )
        rightHost2 = self.addHost( 'server2', mac='99:00:00:00:00:02' )
        rightHost3 = self.addHost( 'proxyArp' )

        # Add links
        self.addLink( leftHost1, leftSwitch )
        self.addLink( leftHost2, leftSwitch )
        self.addLink( leftHost3, leftSwitch )
        self.addLink( leftSwitch, rightSwitch )
        self.addLink( rightSwitch, rightHost1 )
        self.addLink( rightSwitch, rightHost2 )
        self.addLink( rightSwitch, rightHost3 )

topos = { 'mytopo': ( lambda: MyTopo() ) }
