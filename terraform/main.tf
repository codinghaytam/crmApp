provider "aws" {
  region = "eu-west-1"

}

data "aws_ami" "ubuntu" {
  most_recent = true
  owners      = ["099720109477"] # Canonical official owner - avoids Marketplace-only AMIs

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-focal-20.04-amd64-server-*"]
  }

}

resource "aws_vpc" "main" {
  cidr_block = "10.0.0.0/16"
tags = {
    Name = "main-vpc"
  }
}

resource "aws_subnet" "subnet1" {
  vpc_id = aws_vpc.main.id
    cidr_block = "10.0.1.0/24"
    availability_zone = "eu-west-1a"
    map_public_ip_on_launch = true
    tags = {
      Name = "main-subnet-1a"
    }
}



resource "aws_route_table" "table1" {
  vpc_id = aws_vpc.main.id
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.gate.id
  }

}
resource "aws_route_table_association" "public_subnet_1a" {
  subnet_id = aws_subnet.subnet1.id
  route_table_id = aws_route_table.table1.id
}

resource "aws_internet_gateway" "gate" {
    vpc_id = aws_vpc.main.id
  tags = {
    Name = "main-gateway"
  }
}

resource "aws_instance" "backend_server" {
  ami = data.aws_ami.ubuntu.id
  instance_type = "t2.micro"
  subnet_id = aws_subnet.subnet1.id
  vpc_security_group_ids = [aws_security_group.terrafrom_sg.id]
  associate_public_ip_address = true
  tags = {
    Name = "backend-server"
  }
}

resource "aws_instance" "front_server" {
    ami = data.aws_ami.ubuntu.id
    instance_type = "t2.micro"
    subnet_id = aws_subnet.subnet1.id
    vpc_security_group_ids = [aws_security_group.terrafrom_sg.id]
    associate_public_ip_address = true
    tags = {
        Name = "frontend-server"
    }
}

# Request a spot instance at $0.03
resource "aws_instance" "ci-cd" {
  ami           = data.aws_ami.ubuntu.id
  associate_public_ip_address = true
  instance_type = "t2.micro"
  user_data = "./userData.sh"
  key_name = "haytam"
  root_block_device {
    volume_size = 30
    volume_type = "gp3"
  }
  subnet_id = aws_subnet.subnet1.id
  vpc_security_group_ids = [aws_security_group.terrafrom_sg.id]

  tags = {
    Name = "ci-cd-server"
  }
}








output "backend_public_ip" {
  description = "Public IP of backend server"
  value       = aws_instance.backend_server.public_ip
}

output "frontend_public_ip" {
  description = "Public IP of frontend server"
  value       = aws_instance.front_server.public_ip
}

output "ci_cd_public_ip" {
  description = "Public IP of CI/CD server"
  value       = aws_instance.ci-cd.public_ip
}
output "ci_cd_instance_id" {
  description = "instance id of CI/CD server"
    value       = aws_instance.ci-cd.id
}

output "backend_instance_id" {
  description = "Backend instance id"
  value       = aws_instance.backend_server.id
}

output "frontend_instance_id" {
  description = "Frontend instance id"
  value       = aws_instance.front_server.id
}
