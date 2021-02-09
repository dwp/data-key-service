terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 3.0"
    }
  }
}

provider "aws" {
  access_key                  = "access_key_id"
  region                      = "eu-west-2"
  s3_force_path_style         = true
  secret_key                  = "secret_access_key"
  skip_credentials_validation = true
  skip_metadata_api_check     = true
  skip_requesting_account_id  = true

  endpoints {
    s3       = "http://localstack:4566"
  }
}

resource "aws_s3_bucket" "crl_bucket" {
  bucket = "dw-local-crl"
  acl    = "public-read"
}
