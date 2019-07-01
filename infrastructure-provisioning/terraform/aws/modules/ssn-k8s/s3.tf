resource "aws_s3_bucket" "k8s-bucket" {
  bucket = "${var.service_base_name}-ssn-bucket"
  acl    = "private"
  tags = {
    Name = "${var.service_base_name}-ssn-bucket"
  }
  # force_destroy = true
}
