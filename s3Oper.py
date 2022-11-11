import argparse
import boto
from boto.s3.connection import S3Connection
import sys
import yaml
#import dict_digger
bucket = ""
path =""
def GetConnectionDetails( appName):
    with open("s3ConnectionDetails.yaml", 'r') as stream:
        data = yaml.load(stream) 
    appList = data.keys()
    if appName in appList:
    #accessCode = dataam_dikmgger.dig( data, appName, 'aws_access_code')
    #secretKey = dataam_dikmgger.dig( data, appName, 'aws_secret_key')
    #host = dataam_dikmgger.dig( data, appName, 'host')
    #port = dataam_dikmgger.dig( data, appName, 'port')
        accessCode = data[appName]['aws_access_code']
        secretKey = data[appName]['aws_secret_key']
        host =  data[appName]['host']
        port =  data[appName]['port']
        return accessCode, secretKey, host, port
    else:
        sys.exit("Invalid App name") 
     
def GetS3Connection(appName):
    (accessCode, secretKey, host, port) = GetConnectionDetails( appName ) 
    print accessCode, secretKey, host, port
    calling_format = 'boto.s3.connection.ProtocolIndependentOrdinaryCallingFormat'
    debug_level = 0
    #return S3Connection(aws_access_key_id=accessCode, aws_secret_access_key=secretKey, is_secure=True, port=port, host=host, debug=debug_level, calling_format=calling_format) 
    return S3Connection(aws_access_key_id=accessCode, aws_secret_access_key=secretKey, host=host, is_secure=False, calling_format=calling_format) 

def CreateBucket(bucket, s3Conn):
    buckName = bucket.lower()
    print "Creating bucket" 
    s3Conn.create_bucket(buckName)

def listBuckets(s3Conn):
    
    print "Printing list of Buckets"
     
    for bucket in s3Conn.get_all_buckets():
        print "{name}\t{created}".format(
                name = bucket.name,
                created = bucket.creation_date,
        )

def listBucket(s3Conn, bucket):

    myBucket = s3Conn.get_bucket(bucket)
    for rs in myBucket.list("", "/"):
        print rs.name
        print rs.size
    print myBucket.list()
    #print type(bucket)
    
def listBucketPath(s3Conn, bucket, path):
    print "printing files"

def listContents(s3Conn, bucket, path):
    print "Bucket is {bucket}/t Path is {path}".format( bucket = bucket, path = path, ) 
    if bucket  and  path :
        print "I am here"
        listBucketPath(s3Conn, bucket, path)
    elif bucket.strip():
        listBucket(s3Conn, bucket) 
    else:
        listBuckets(s3Conn)
     
def main() :
    """ Read input argumnets 
    """
    bucket=""
    parser = argparse.ArgumentParser()
    parser.add_argument( '--appname', help="Name of application ex: fffff", required=True , dest="appName" )
    parser.add_argument( '--action', help="Action (list [ --bucket <bucket name>[ --path <path>]] | create --bucket <bucket name> )", required=True , dest="action" , choices = ['list', 'create'] )
    parser.add_argument( '--bucket', help=" Bucket name --bucket <bucket name>", required=False , dest="bucket" )
    #parser.add_argument( '--bucket',  required=False , dest="bucket" )
    parser.add_argument( '--path', help="Path to list files --path <path name>", required=False , dest="path" )
    
    #print appName
    args = parser.parse_args()
    s3Conn = GetS3Connection(args.appName)

    if args.action.lower() == "create" :
        print args.action.lower() 
        if args.bucket == "" :
            parser.error( "Bucket name is required")
        else:
            CreateBucket( args.bucket, s3Conn )
    elif args.action.lower() == "list" :        
        listContents(s3Conn, args.bucket, args.path)

    print args.appName
    #s3Conn = GetS3Connection(args.appName)
    

if __name__ == "__main__":
    main()
