resource "digitalocean_droplet" "worker1" {
  image = "docker-20-04"
  name = "worker1" 
  region = "fra1"
  size = "s-1vcpu-1gb"
  ssh_keys = [
    data.digitalocean_ssh_key.ChrisMajor.id
  ]

  connection {
    host = self.ipv4_address
    user = "root"
    type = "ssh"
    private_key = file(var.pvt_key)
    timeout = "2m"
  }

  provisioner "remote-exec" {
    inline = [
      "export PATH=$PATH:/usr/bin",
      # install nginx
      "sudo apt update",
      "sudo apt install -y nginx",
      "docker run -d --name minitwit -it magmose1/minitwitimage:latest -d",
      # "docker run -d --name minitwit -it magmose1/minitwitimagedev:latest -d",
      "sudo ufw allow 8080",
      "sudo ufw allow 8081",
      "sudo ufw allow 8082",
      "sudo ufw allow 8083",
      "curl ifconfig.me"
    ]
  }
}