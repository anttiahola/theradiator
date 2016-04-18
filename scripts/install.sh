#!/bin/bash
sudo stop TheRadiator
./activator stage
sudo rm -rf /opt/TheRadiator-0.1
sudo cp -r target/universal/stage /opt/TheRadiator-0.1
sudo cp TheRadiator.conf /etc/init/
sudo cp $1 /opt/TheRadiator-0.1/conf/application.conf
sudo chown -R pi:users /opt/TheRadiator-0.1
sudo start TheRadiator
sudo tail -f /var/log/upstart/TheRadiator.log