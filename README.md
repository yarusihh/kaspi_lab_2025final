# kaspi_lab_2025final

Полная структура проекта:

```
kaspi_lab_2025final/
│
├── docker-compose.yml
├── README.md
├── .gitignore
│
├── docker/                 # все что связано с infra wiring
│   ├── prometheus/
│   │   └── prometheus.yml
│   ├── grafana/
│   │   └── provisioning/
│   └── scripts/
│       ├── init-minio.sh
│       ├── init-kafka.sh
│       └── backup.sh
│
├── infra_data/             # bind volumes (в git только пустые папки)
│   ├── postgres/
│   │   └── .gitkeep
│   ├── minio/
│   │   └── .gitkeep
│   ├── kafka/
│   │   └── .gitkeep
│   ├── grafana/
│   │   └── .gitkeep
│   └── prometheus/
│       └── .gitkeep
│
├── proj1/
│   ├── Dockerfile
│   └── app/
│
├── proj2_1/
│   ├── Dockerfile
│   └── app/
│
├── proj2_2/
│   ├── Dockerfile
│   └── app/
│
```
