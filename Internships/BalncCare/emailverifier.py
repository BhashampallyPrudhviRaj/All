import re
import smtplib
import dns.resolver

# Address used for SMTP MAIL FROM command  
fromAddress = 'bprudhviraj77@gmail.com'

# Simple Regex for syntax checking
regex = '^[_a-z0-9-]+(\.[_a-z0-9-]+)*@[a-z0-9-]+(\.[a-z0-9-]+)*(\.[a-z]{2,})$'

# Email address to verify
c=0
try:
	h1 = open("D:/Desktop/SREC/Internships/Balnc Care/Mails/Net/10millionemail/10millionemail/google_5000000.txt", "r")
	f1 = h1.readlines()
	h2 = open("mails1.csv", "w")
	"""match_list = [ ]
	for a in f1:
		match_list = re.findall(r'[_a-z0-9-]+(\.[_a-z0-9-]+)*@[a-z0-9-]+(\.[a-z0-9-]+)*(\.[a-z]{2,})', a)
	    #match_list = re.findall(r'[\w\.-]+@[\w\.-]+', a)
		if match_list:
			for x in match_list:	
				print("Success")
				b = write(x+"\r\n") 
				inputAddress = b"""
	for x in f1:
				#inputAddress = input('Please enter the emailAddress to verify:')
				#addressToVerify = str(inputAddress)
				addressToVerify = str(x)

# Syntax check
				match = re.match(regex, addressToVerify)
				if match == None:
					print('Bad Syntax')
					raise ValueError('Bad Syntax')

# Get domain for DNS lookup
				splitAddress = addressToVerify.split('@')
				domain = str(splitAddress[1])
				#print('Domain:', domain)

				# MX record lookup
				records = dns.resolver.query('emailhippo.com', 'MX')
				#records = dns.resolver.query(domain, 'MX')
				mxRecord = records[0].exchange
				mxRecord = str(mxRecord)


				# SMTP lib setup (use debug level for full output)
				server = smtplib.SMTP()
				server.set_debuglevel(0)

				# SMTP Conversation
				server.connect(mxRecord)
				server.helo(server.local_hostname) ### server.local_hostname(Get local server hostname)
				server.mail(fromAddress)
				code, message = server.rcpt(str(addressToVerify))
				server.quit()

				#print(code)
				#print(message)

				# Assume SMTP response 250 is success
				if code == 250:
					#h2.write(b+"\r\n")
					h2.write(x)
					c+=1
					#print("Success")
	print("Count : ",c)
except Exception as e:
	print("Error : ",e)

"""else:
					print('Bad')"""