CREATE TABLE booking_idempotency (
    idem_key UUID PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES app_user(id),
    slot_id BIGINT NOT NULL REFERENCES slot(id),
    appointment_id BIGINT REFERENCES appointment(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
