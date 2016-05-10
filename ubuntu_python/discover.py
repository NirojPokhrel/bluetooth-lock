from bluetooth import *
#import bluetooth
import os
import time
import subprocess

target_name = "XT1052"
target_address = None
MY_UUID = "5f6289aa-0acc-11e6-b512-3e1d05defe78"

def protocol_1():
	'''
	The bluetooth from the laptop will search the device continuously every 30 seconds. If the device is in range
	than it's fine and when the device is out of range then the PC will logout automatically.
	'''
	while(True):
		nearby_devices = bluetooth.discover_devices()
		target_address = None
		for bdaddr in nearby_devices:
			if target_name == bluetooth.lookup_name( bdaddr ):
				target_address = bdaddr
				break
			else:
				target_address = None

		if target_address is not None:
			print "found target bluetooth device with address ", target_address
		else:
			print "could not find target bluetooth device nearby"
			subprocess.call(["gnome-screensaver-command", "-l"])
			break
		time.sleep(30)

	print "Program Exit!!!"

def server_protocol():

	server_sock = BluetoothSocket(RFCOMM)
	'''
	Following steps is to prevent the unseen situations when
	the port are free but in the time of milliseconds another
	device will acquire the port and we will get the exception.
	'''
	i = 23
	while True:
		#free_port = get_available_port( RFCOMM )
		i = i + 1
		free_port = i
		try:
			server_sock.bind(("", free_port))
			break
		except BluetoothError:
			print "Couldn't bind to ",free_port
	server_sock.listen(1)
	print i
	advertise_service(server_sock, "BluLock", 
		service_id=MY_UUID, service_classes = [ MY_UUID ] )

	print "Before accepting the client:"
	client_sock, client_info = server_sock.accept()
	print "Accepted connection from ", client_info

	while True:
		data = client_sock.recv(1024)
		print "received [%s]" % data
		if data == 'sleep':
			subprocess.call(["gnome-screensaver-command", "-l"])
		elif data == 'shutdown':
			subprocess.call(["shutdown", "now"])
		elif data == 'getfiles':
			#reply_data = subprocess.call(["ls", "files/"])
			reply_data = get_file_names()
			print reply_data
			client_sock.send(reply_data)

	client_sock.close()
	server_sock.close()

def test():
	print SERIAL_PORT_CLASS

def get_file_names():
	f=os.popen("ls files/")
	output_data = ''
	for i in f.readlines():
		output_data += i
	return output_data

if __name__ == "__main__":
	server_protocol()

#server_sock = bluetooth.BluetoothSocket(bluetooth.RFCOMM)
#port = 1
#server_sock.bind(("",port))
#server_sock.listen(1)
#client_sock, address = server_sock.accept()
#print "Accepted connection from ", address

#data = client_sock.recv(1024)
#print "received [%s]" % data

#client_sock.close()
#server_sock.close()