-- Rebuild the application schema. DROP TABLE statements delete existing table data.
-- Review the target database and back up its contents before running this script.
-- ============================================================
--   mysql -h <mysql-host> -u root -p < schema.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS shardchain_db
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

USE shardchain_db;

-- ------------------------------------------------------------
-- ------------------------------------------------------------
DROP TABLE IF EXISTS agent;
CREATE TABLE agent (
    agent_id          VARCHAR(64)  NOT NULL COMMENT 'Agent id',
    organization_id   VARCHAR(64)  NOT NULL COMMENT 'Organization id',
    current_shard_id  INT          NULL     COMMENT 'Current shard id',
    public_key        VARCHAR(256) NOT NULL COMMENT 'Public key',
    status            VARCHAR(16)  NOT NULL DEFAULT 'OFFLINE' COMMENT 'Status',
    PRIMARY KEY (agent_id),
    KEY idx_org (organization_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Agent table';

-- ------------------------------------------------------------
-- ------------------------------------------------------------
DROP TABLE IF EXISTS coordinator;
CREATE TABLE coordinator (
    coordinator_id    VARCHAR(64) NOT NULL COMMENT 'Coordinator id',
    current_epoch     BIGINT      NULL     COMMENT 'Current epoch',
    epoch_start_time  BIGINT      NULL     COMMENT 'Epoch start time',
    epoch_duration    BIGINT      NULL     COMMENT 'Epoch duration',
    global_state      VARCHAR(16) NOT NULL DEFAULT 'RUNNING' COMMENT 'Global state',
    total_shards      INT         NULL     COMMENT 'Total shards',
    PRIMARY KEY (coordinator_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Coordinator table';

-- ------------------------------------------------------------
-- ------------------------------------------------------------
DROP TABLE IF EXISTS shard;
CREATE TABLE shard (
    shard_id          INT      NOT NULL AUTO_INCREMENT COMMENT 'Shard id',
    ip_address        VARCHAR(64)  NOT NULL COMMENT 'Ip address',
    port              INT      NOT NULL COMMENT 'Port',
    cpu_usage         DOUBLE   NULL     COMMENT 'Cpu usage',
    memory_usage      DOUBLE   NULL     COMMENT 'Memory usage',
    disk_io_load      DOUBLE   NULL     COMMENT 'Disk io load',
    network_bandwidth DOUBLE   NULL     COMMENT 'Network bandwidth',
    current_tps       BIGINT   NULL     COMMENT 'Current tps',
    PRIMARY KEY (shard_id),
    UNIQUE KEY uk_ip_port (ip_address, port)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Shard table';

-- ------------------------------------------------------------
-- ------------------------------------------------------------
DROP TABLE IF EXISTS chain_request;
CREATE TABLE chain_request (
    request_id        VARCHAR(64)   NOT NULL COMMENT 'Request id',
    tx_type           VARCHAR(16)   NOT NULL COMMENT 'Tx type',
    sender_address    VARCHAR(256)  NOT NULL COMMENT 'Sender address',
    receiver_address  VARCHAR(256)  NULL     COMMENT 'Receiver address',
    asset_id          VARCHAR(128)  NULL     COMMENT 'Asset id',
    amount            DECIMAL(30, 18) NULL   COMMENT 'Amount',
    target_shard_id   INT           NULL     COMMENT 'Target shard id',
    timestamp         BIGINT        NOT NULL COMMENT 'Timestamp',
    signature         VARCHAR(512)  NULL     COMMENT 'Signature',
    payload           TEXT          NULL     COMMENT 'Payload',
    PRIMARY KEY (request_id),
    KEY idx_sender (sender_address),
    KEY idx_target_shard (target_shard_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Chain_request table';

-- ------------------------------------------------------------
-- ------------------------------------------------------------
DROP TABLE IF EXISTS chain_receipt;
CREATE TABLE chain_receipt (
    request_id      VARCHAR(64) NOT NULL COMMENT 'Request id',
    tx_hash         VARCHAR(128) NULL    COMMENT 'Tx hash',
    status          TINYINT     NOT NULL DEFAULT 0 COMMENT 'Status',
    block_number    BIGINT      NULL     COMMENT 'Block number',
    gas_used        BIGINT      NULL     COMMENT 'Gas used',
    error_message   TEXT        NULL     COMMENT 'Error message',
    logs            TEXT        NULL     COMMENT 'Logs',
    timestamp       BIGINT      NULL     COMMENT 'Timestamp',
    PRIMARY KEY (request_id),
    KEY idx_tx_hash (tx_hash)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Chain_receipt table';

-- ------------------------------------------------------------
-- ------------------------------------------------------------
DROP TABLE IF EXISTS task;
CREATE TABLE task (
    task_id           VARCHAR(64) NOT NULL COMMENT 'Task id',
    request_id        VARCHAR(64) NULL     COMMENT 'Request id',
    task_type         VARCHAR(32) NOT NULL COMMENT 'Task type',
    source_shard_id   INT         NULL     COMMENT 'Source shard id',
    target_shard_id   INT         NULL     COMMENT 'Target shard id',
    status            VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'Status',
    create_time       BIGINT      NOT NULL COMMENT 'Create time',
    PRIMARY KEY (task_id),
    KEY idx_request (request_id),
    KEY idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Task table';

-- ------------------------------------------------------------
-- ------------------------------------------------------------
DROP TABLE IF EXISTS chain_transcription;
CREATE TABLE chain_transcription (
    request_id         VARCHAR(64)    NOT NULL COMMENT 'Request id',
    tx_hash            VARCHAR(128)   NULL     COMMENT 'Tx hash',
    tx_type            VARCHAR(16)    NOT NULL COMMENT 'Tx type',
    sender_address     VARCHAR(256)   NOT NULL COMMENT 'Sender address',
    receiver_address   VARCHAR(256)   NULL     COMMENT 'Receiver address',
    asset_id           VARCHAR(128)   NULL     COMMENT 'Asset id',
    amount             DECIMAL(30, 18) NULL    COMMENT 'Amount',
    sender_shard_id    INT            NULL     COMMENT 'Sender shard id',
    receiver_shard_id  INT            NULL     COMMENT 'Receiver shard id',
    target_shard_id    INT            NULL     COMMENT 'Target shard id',
    cross_shard        TINYINT        NOT NULL DEFAULT 0 COMMENT 'Cross shard',
    route_source       VARCHAR(16)    NULL     COMMENT 'Route source',
    source             VARCHAR(16)    NOT NULL COMMENT 'Source',
    processor          VARCHAR(64)    NULL     COMMENT 'Processor',
    submit_time        BIGINT         NULL     COMMENT 'Submit time',
    route_time         BIGINT         NULL     COMMENT 'Route time',
    cross_check_time   BIGINT         NULL     COMMENT 'Cross check time',
    receipt_time       BIGINT         NULL     COMMENT 'Receipt time',
    total_latency      BIGINT         NULL     COMMENT 'Total latency',
    recorded_on_chain  TINYINT        NOT NULL DEFAULT 0 COMMENT 'Recorded on chain',
    on_chain_tx_hash   VARCHAR(128)   NULL     COMMENT 'On chain tx hash',
    block_number       BIGINT         NULL     COMMENT 'Block number',
    gas_used           BIGINT         NULL     COMMENT 'Gas used',
    status             TINYINT        NOT NULL DEFAULT 1 COMMENT 'Status',
    error_message      TEXT           NULL     COMMENT 'Error message',
    logs               TEXT           NULL     COMMENT 'Logs',
    create_time        BIGINT         NOT NULL COMMENT 'Create time',
    PRIMARY KEY (request_id),
    KEY idx_cross_shard (cross_shard),
    KEY idx_target_shard (target_shard_id),
    KEY idx_source (source),
    KEY idx_block_number (block_number),
    KEY idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Chain_transcription table';
