package io.certifico.app.data.store;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import io.certifico.app.data.cert.BlockCert;
import io.certifico.app.data.model.CertificateRecord;
import io.certifico.app.data.store.cursor.CertificateCursorWrapper;

import java.util.ArrayList;
import java.util.List;

public class CertificateStore implements DataStore {

    private SQLiteDatabase mDatabase;

    public CertificateStore(LMDatabaseHelper databaseHelper) {
        mDatabase = databaseHelper.getWritableDatabase();
    }

    public List<CertificateRecord> loadCertificates() {
        List<CertificateRecord> certificates = new ArrayList<>();
        Cursor cursor = mDatabase.query(
                LMDatabaseHelper.Table.CERTIFICATE,
                null,
                null,
                null,
                null,
                null,
                null);

        if (cursor.moveToFirst()) {
            CertificateCursorWrapper cursorWrapper = new CertificateCursorWrapper(cursor);
            while (!cursorWrapper.isAfterLast()) {
                certificates.add(cursorWrapper.getCertificate());
                cursorWrapper.moveToNext();
            }
        }

        cursor.close();

        return certificates;
    }

    public CertificateRecord loadCertificate(String certUuid) {
        CertificateRecord certificate = null;
        Cursor cursor = mDatabase.query(
                LMDatabaseHelper.Table.CERTIFICATE,
                null,
                LMDatabaseHelper.Column.Certificate.UUID + " = ? ",
                new String[] { certUuid },
                null,
                null,
                null);

        if (cursor.moveToFirst()) {
            CertificateCursorWrapper cursorWrapper = new CertificateCursorWrapper(cursor);
            certificate = cursorWrapper.getCertificate();
        }

        cursor.close();

        return certificate;
    }

    public List<CertificateRecord> loadCertificatesForIssuer(String issuerUuid) {
        List<CertificateRecord> certificateList = new ArrayList<>();

        Cursor cursor = mDatabase.query(
                LMDatabaseHelper.Table.CERTIFICATE,
                null,
                LMDatabaseHelper.Column.Certificate.ISSUER_UUID + " = ?",
                new String[] { issuerUuid },
                null,
                null,
                null);

        if (cursor.moveToFirst()) {
            CertificateCursorWrapper cursorWrapper = new CertificateCursorWrapper(cursor);
            while (!cursorWrapper.isAfterLast()) {
                CertificateRecord certificate = cursorWrapper.getCertificate();
                certificateList.add(certificate);
                cursorWrapper.moveToNext();
            }
        }

        cursor.close();

        return certificateList;
    }

    public void saveBlockchainCertificate(BlockCert blockCert) {
        String certUid = blockCert.getCertUid();
        String urlString = blockCert.getUrl();
        String issuerId = blockCert.getIssuerId();
        String certName = blockCert.getCertName();
        String certDescription = blockCert.getCertDescription();
        String issueDate = blockCert.getIssueDate();
        String metadata = blockCert.getMetadata();
        String expirationDate = blockCert.getExpirationDate();

        ContentValues contentValues = new ContentValues();

        contentValues.put(LMDatabaseHelper.Column.Certificate.UUID, certUid);
        contentValues.put(LMDatabaseHelper.Column.Certificate.NAME, certName);
        contentValues.put(LMDatabaseHelper.Column.Certificate.DESCRIPTION, certDescription);
        contentValues.put(LMDatabaseHelper.Column.Certificate.ISSUER_UUID, issuerId);
        contentValues.put(LMDatabaseHelper.Column.Certificate.ISSUE_DATE, issueDate);
        contentValues.put(LMDatabaseHelper.Column.Certificate.URL, urlString);
        contentValues.put(LMDatabaseHelper.Column.Certificate.EXPIRATION_DATE, expirationDate);
        contentValues.put(LMDatabaseHelper.Column.Certificate.METADATA, metadata);

        if (loadCertificate(certUid) == null) {
            mDatabase.insert(LMDatabaseHelper.Table.CERTIFICATE,
                    null,
                    contentValues);
        } else {
            mDatabase.update(LMDatabaseHelper.Table.CERTIFICATE,
                    contentValues,
                    LMDatabaseHelper.Column.Certificate.UUID + " = ? ",
                    new String[] { certUid });
        }
    }

    public boolean deleteCertificate(String uuid) {
        // the delete operation should remove 1 row from the table
        return 1 == mDatabase.delete(LMDatabaseHelper.Table.CERTIFICATE,
                LMDatabaseHelper.Column.Certificate.UUID + " = ? ",
                new String[] { uuid });
    }

    @Override
    public void reset() {
        mDatabase.delete(LMDatabaseHelper.Table.CERTIFICATE, null, null);
    }
}
