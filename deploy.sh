docker run -it 340336286833.dkr.ecr.eu-central-1.amazonaws.com/deploy-tools:latest  ansible-playbook -i hosts dex-testnet-playbook.yml -e="branch_name=master" --limit plix_dex_testnet
docker run -it 340336286833.dkr.ecr.eu-central-1.amazonaws.com/deploy-tools:latest  ansible-playbook -i hosts testnet-playbook.yml -e="branch_name=master" --limit plix_nodes_testnet
