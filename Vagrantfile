# -*- mode: ruby -*-
# vi: set ft=ruby :

# All Vagrant configuration is done below. The "2" in Vagrant.configure
# configures the configuration version (we support older styles for
# backwards compatibility). Please don't change it unless you know what
# you're doing.
Vagrant.configure("2") do |config|
  
  if Vagrant.has_plugin?("vagrant-vbguest")
    config.vbguest.auto_update = false
  end

  BOX = "treehouses/buster64"
  BOX_VERSION = "0.13.25"

  config.vm.define "remote" do |remote|
    remote.vm.box = BOX
    remote.vm.box_version = BOX_VERSION

    remote.vm.hostname = "remote"

    remote.vm.provider "virtualbox" do |vb|
      vb.name = "remote"
      vb.memory="666"
    end

    remote.vm.network "forwarded_port", guest: 22, host: 2222, host_ip: "0.0.0.0", id: "ssh", auto_correct: true

    remote.ssh.shell = "bash -c 'BASH_ENV=/etc/profile exec bash'"
    remote.vm.provision "shell", inline: <<-SHELL
      ln -sr /vagrant /root/remote
      ln -sr /vagrant /home/vagrant/remote
      #windows
      dos2unix /vagrant/*/*/*/*/*/*/*/*/*/* /vagrant/*/*/*/*/*/*/*/*/* /vagrant/*/*/*/*/*/*/*/*/* /vagrant/*/*/*/*/*/*/*/* /vagrant/*/*/*/*/*/*/* /vagrant/*/*/*/*/*/* /vagrant/*/*/*/*/* /vagrant/*/*/*/* /vagrant/*/*/* /vagrant/*/* /vagrant/*
      unix2dos /vagrant/gradlew.bat
      #codeclimate
      docker pull codeclimate/codeclimate
      wget https://raw.githubusercontent.com/codeclimate/codeclimate/master/codeclimate-wrapper -O /usr/local/bin/codeclimate
      chmod +x /usr/local/bin/codeclimate
      #mobsf documentation
      echo 'MobSF USAGE\n-----------' >> mobsf-README
      echo 'During Vagrant setup, a MobSF server was initiated with the command:  docker run -itd -p 8000:8000 opensecurity/mobile-security-framework-mobsf:latest\n' >> mobsf-README
      echo 'With MobSF, you are able to run a static analysis on the remote source code with the following steps\n\n' >> mobsf-README
      echo '0.  Retrieve and store your API key by running:  wget http://localhost:8000/api_docs; MOBSF_API_KEY=$\(grep "REST API Key" api_docs\); MOBSF_API_KEY=$\{MOBSF_API_KEY:42:64\}; rm api_docs\n' >> mobsf-README
      echo '1.  Zip the source code for app/ directory inside remote/ and compute the hash:  zip -d source_code app/; HASH=$\(md5sum source_code.zip\); HASH=\$\{HASH:0:32\}\n' >> mobsf-README
      echo '2.  Upload the file to MobSF:  curl -F \"file=\@source_code.zip\" http://localhost:8000/api/v1/upload -H \"Authorization:$MOBSF_API_KEY\"\n' >> mobsf-README
      echo '3.  Perform the security scan:  curl -X POST --url http://localhost:8000/api/v1/scan --data \"scan_type=zip\&file_name=source_code.zip\&hash=$HASH\" -H \"Authorization:\$MOBSF_API_KEY\"\n' >> mobsf-README
      echo '4.  Download the results as PDF:  curl -X POST --url http://localhost:8000/api/v1/download_pdf --data \"hash=$HASH\" -H \"Authorization:$MOBSF_API_KEY\" --output mobsf-security-scan.pdf' >> mobsf-README
      echo '\nEnjoy! :)' >> mobsf-README
    SHELL

    # Run binding on each startup make sure the mount is available on VM restart
    remote.vm.provision "shell", run: "always", inline: <<-SHELL
      docker pull codeclimate/codeclimate
      echo
      echo
      docker pull opensecurity/mobile-security-framework-mobsf
      docker run -itd -p 8000:8000 opensecurity/mobile-security-framework-mobsf:latest
      echo
      echo
      echo
      echo "CODECLIMATE USAGE"
      echo "vagrant ssh"
      echo "cd remote"
      echo "git checkout <branch>"
      echo "codeclimate help"
      echo
      echo
      echo "MOBSF USAGE"
      echo "vagrant ssh"
      echo "cat mobsf-README"
      echo
      echo
      echo
    SHELL
  end
end
