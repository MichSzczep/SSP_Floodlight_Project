# SSP_Floodlight_Project

Uruchomienie minineta z nasza topologia oraz kontrolerem:
sudo mn --custom topo_projekt.py --topo=mytopo --controller=remote,ip=<<ipaddress>>,port=6653 --mac
  
Adres NAT serwera to 10.0.0.4 i tylko pingujac/wget'ujac na ten adres możliwe jest pobieranie zawartości
