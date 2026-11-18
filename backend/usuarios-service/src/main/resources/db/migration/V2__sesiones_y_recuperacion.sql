CREATE TABLE sesiones (
    id UUID PRIMARY KEY,
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    acceso_hash VARCHAR(64) NOT NULL UNIQUE,
    renovacion_hash VARCHAR(64) NOT NULL UNIQUE,
    acceso_vence_en TIMESTAMP WITH TIME ZONE NOT NULL,
    renovacion_vence_en TIMESTAMP WITH TIME ZONE NOT NULL,
    creado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX sesiones_usuario ON sesiones(usuario_id);

CREATE TABLE recuperaciones (
    id UUID PRIMARY KEY,
    usuario_id UUID NOT NULL UNIQUE REFERENCES usuarios(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    vence_en TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE control_administracion (
    id INTEGER PRIMARY KEY CHECK (id = 1)
);
INSERT INTO control_administracion(id) VALUES (1);
