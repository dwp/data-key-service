# Requirements for Data Key Service

## Runtime
* AWS Access via an API Key
* Java 8 JRE 
* CentOS


## Infrastructure 
* KMS CMK (Master Key), (enabled)
* Parameter Store parameter 
  * named ```data_key_service.currentKeyId```
  * value set to the full ARN for the KMS master key
* AWS user must have permissions for:
  * ```ssm:GetParameter``` on the Parameter store parameter
  * Create Data Key, Encrypt Data Key, Decrypt Data Key for the KMS CMK
  
