export const db = {
    userName: 'codingkapoor',
    password: 'codingkapoor',
    database: 'push-notifications',
    table: 'tokens'
}

export const kafkaConfig = {
    interface: 'localhost',
    port: 2181,
    consumer : {
        topic: 'employee',
        partition: 0
    }
}

export const expressServer = {
    port: 3000
}
