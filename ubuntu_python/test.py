import os

def get_file_names():
	f=os.popen("ls files/")
	output_data = ''
	for i in f.readlines():
		output_data += i
	print output_data

if __name__ == "__main__":
	get_file_names()