from pyspark.sql import SQLContext,SparkSession
from pyspark.sql.functions import col, lit
import datetime
from credentials import *
 
# Helper function to load a cassandra table to a dataframe
def readCassandra(spark,keyspace,table):
  return spark.read \
    .format("org.apache.spark.sql.cassandra") \
    .option("spark.cassandra.auth.username",cassandra_user) \
    .option("spark.cassandra.auth.password",cassandra_password) \
    .option("table",table) \
    .option("keyspace",keyspace) \
    .load()
 
# Helper function to wrote a cassandra table from a dataframe
def writeCassandra(df,keyspace,table):
  return df.write \
    .format("org.apache.spark.sql.cassandra") \
    .option("spark.cassandra.auth.username",cassandra_user) \
    .option("spark.cassandra.auth.password",cassandra_password) \
    .option("table",table) \
    .option("keyspace",keyspace) \
    .save(mode="append")
 
# Helper function to read a cassandra table to a dataframe
def readOracle(spark,url,query):
  return spark.read \
    .format("jdbc") \
    .option("url", url) \
    .option("dbtable", query) \
    .option("user", oracle_user) \
    .option("password", oracle_password) \
    .option("driver", "oracle.jdbc.driver.OracleDriver") \
    .load()
 
# Helper function to read a cassandra table to a dataframe
def writeOracle(df,url,table):
  return df.write \
    .format("jdbc") \
    .option("url", url) \
    .option("dbtable", table) \
    .option("user", oracle_user) \
    .option("password", oracle_password) \
    .option("driver", "oracle.jdbc.driver.OracleDriver") \
    .mode("append")’’’
