from pyspark import SparkContext, SparkConf
from pyspark.sql import SQLContext,SparkSession,Window
from pyspark.sql.functions import *
from spark_helper import *
import pyspark.sql.functions as F
 
spark = SparkSession.builder \
  .appName("vasp - purging") \
  .config("spark.memory.fraction", 0.8) \
  .config("spark.executor.memory", "16g") \
  .config("spark.driver.memory", "16g")\
  .config('spark.sql.session.timeZone', 'UTC')\
  .config('spark.executor.instances',"1") \
  .config('spark.sql.autoBroadcastJoinThreshold',-1) \
  .getOrCreate()
 
 
df_read_archive = readCassandra(spark,"vasp_jetfire","subscribers_audit") \
              .where( \
             ((col("created_dttm")>="2020-04-01 00:00:00.000000001") & (col("created_dttm")<="2020-04-16 59:59.999999999")) \
                 ) \
               .select( \
               col("id"), \
               col("api_source"), \
               col("api_status"), \
               col("audit_api_name"), \
               col("audit_db_operation"), \
               col("audit_impact"), \
               col("correlation_id"), \
               col("cpid"), \
               col("cpid_id"), \
               col("created_dttm"), \
               col("customer_type"), \
               col("deleted_dttm"), \
               col("email_id"), \
               col("msisdn"), \
               col("operation_status"), \
               col("operator_id"), \
               col("partner_id"), \
               col("plan_id"), \
               col("reason"), \
               col("registration_dttm"), \
               col("registration_status"), \
               col("request_payload"), \
               col("response_payload"), \
               col("scheduler_status"), \
               col("status_code"), \
               col("subscriber_id"), \
               col("subscription_status"), \
               col("suspended_dttm") \
               )
 
 
df_write = writeCassandra(df_read_archive,"vasp_jetfire","subscribers_audit_archive")
#df_read_archive.coalesce(1).write.csv('delete_vasp',mode="overwrite")
#print df_read_archive.count()
spark.stop()
