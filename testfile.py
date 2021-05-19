 cat test1.py testjason.py
serverandclusters ={}
with open('serversandclusters.txt','r') as f:
     for read_data in f.readlines():
         clulist = read_data.split()
         for i in range(1, len(clulist)):
             serverandclusters.setdefault(clulist[i], []).append(clulist[0])
"""
             if clulist[i] in serverandclusters.keys():
                 serverandclusters[clulist[i]].append(clulist[0])
             else:
                 serverandclusters[clulist[i]] = clulist[0]
"""

for (k, v) in sorted(serverandclusters.items()):
    print(k , v, len(v))
     #sernm = read_data.split[0]
     #print(read_data)
import json

# some JSON:
x =  '{ "person1": { "name":"John", "age":30, "city":{"cityname":"New York", "zip":"2222"}},"person2": { "name":"John", "age":31, "city":"New York"}}'

# parse x:
y = json.loads(x)

persons = y.keys()
print (y["person1"]["age"])
print (y["person1"]["city"])
for person in persons:
# the result is a Python dictionary:
    print(y[person]["age"])

with open('response_card_trck_rltm_wtr_mrk_0425.json','r') as f:
    read_data = json.load(f,strict=False)
    servers = read_data.keys()
    #print servers
    print(read_data["esg6l1702"][4])
    for server in servers:
        stdout1 = read_data[server]
        #stdout2 = stdout1["stdout"]

print(type(stdout1))
print(stdout1)
print(type(stdout1["stdout"]))
print(stdout1["stdout"])
