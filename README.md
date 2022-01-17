# SSP_Floodlight_Project

Uruchomienie minineta z nasza topologia oraz kontrolerem:
sudo mn --custom topo_projekt.py --topo=mytopo --controller=remote,ip=<<ipaddress>>,port=6653 --mac
  
Adres NAT serwera to 10.0.0.4 i tylko pingujac/wget'ujac na ten adres możliwe jest pobieranie zawartości
  
Co i jak trzeba uruchomić:
  
  1. Mininet jak wyżej
  2. W mininecie na hostach server1 oraz server 2 uruchomić serwer http poprzez polecenie "python3 -m http.server"
  3. Uruchomić kontroler
  4. Na hostach podpisanych jako client1, client2, client3 uruchomić skrypt traffic.sh: ./traffic.sh
  5. Patrzeć jak wszystko pięknie działa :)
