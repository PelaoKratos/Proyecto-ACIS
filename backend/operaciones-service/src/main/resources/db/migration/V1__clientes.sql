CREATE TABLE clientes (
    id UUID PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL CHECK (length(trim(nombre)) > 0),
    nombre_normalizado VARCHAR(120) NOT NULL UNIQUE,
    usuario_id UUID UNIQUE,
    horas_llegada INTEGER,
    horas_resolucion INTEGER,
    creado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    CONSTRAINT acuerdo_completo CHECK (
        (horas_llegada IS NULL AND horas_resolucion IS NULL) OR
        (horas_llegada IS NOT NULL AND horas_resolucion IS NOT NULL AND
         horas_llegada BETWEEN 1 AND 720 AND horas_resolucion BETWEEN 1 AND 720 AND
         horas_resolucion >= horas_llegada)
    )
);
