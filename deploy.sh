#!/bin/bash  

function help() {
	echo "Usage: deploy.sh [beta|prod|test]"	
}


function build_up() {
	mvn clean
	mvn package -Dmaven.test.skip=true
}


function upload_to_prod() {
    if [ "$1" == "s3" ]; then
	echo 's3'
    	aws s3 cp ./target/ufoto-cloud-gateway-1.0-SNAPSHOT.jar s3://test.ufotosoft.com/prod-package/ufoto-cloud-gateway-1.0-SNAPSHOT.jar  --acl public-read
    else
	scp -i "us_ufoto.pem" ./target/ufoto-cloud-gateway-1.0-SNAPSHOT.jar ubuntu@54.163.81.128:~/
    ssh -i "us_ufoto.pem" ubuntu@54.163.81.128 "bash /home/ubuntu/publish_ufoto_cloud_gateway.sh"
   fi
}

function upload_to_beta() {
    if [ "$1" == "s3" ]; then
	echo 's3'
    	aws s3 cp ./target/ufoto-cloud-gateway-1.0-SNAPSHOT.jar s3://test.ufotosoft.com/beta-package/ufoto-cloud-gateway-1.0-SNAPSHOT.jar  --acl public-read
    	ssh -i "us_ufoto.pem" ubuntu@54.208.210.215 "bash /home/ubuntu/publish_ufoto_cloud_gateway.sh s3"
    else
	echo 'scp'
    	scp -i "us_ufoto.pem" ./target/ufoto-cloud-gateway-1.0-SNAPSHOT.jar ubuntu@54.208.210.215:~/
    	ssh -i "us_ufoto.pem" ubuntu@54.208.210.215 "bash /home/ubuntu/publish_ufoto_cloud_gateway.sh"
    fi
}

function upload_to_test() {
	scp ./target/ufoto-cloud-gateway-1.0-SNAPSHOT.jar ufoto@192.168.60.199:~/
}

function beta_deploy() {
    echo "============START==========="
    
    build_up
    upload_to_beta "$1"
    
    echo "============E N D==========="
}

function prod_deploy() {
    echo "============START==========="

    build_up
    upload_to_prod "$1"

    echo "============E N D==========="
}
function test_deploy() {
    echo "============START==========="

    #build_up
    upload_to_test

    echo "============E N D==========="
}

if [ $# -eq 0 ]; then
    echo -e "No commands provided. Defaulting to [beta]\n"
    beta_deploy
    exit 0
fi

case "$1" in
"beta")
    beta_deploy "$2"
    ;;
"prod")
    prod_deploy "$2"
    ;;   
"test")
    test_deploy
    ;;   
*)
    help
    ;;
esac

