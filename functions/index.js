const functions = require('firebase-functions');
const twilio = require('twilio');

exports.sendSMS = functions.region('europe-west2').https.onCall(async (data, context) => {
    try {
        const accountSid = functions.config().twilio.sid;
        const authToken = functions.config().twilio.token;
        const client = new twilio(accountSid, authToken);

        const message = await client.messages.create({
            body: `Holiday Details: ${data.message}`,
            to: data.to,
            from: '+447360274311'
        });

        console.log(message.sid);
        return { status: 'success' };
    } catch (error) {
        console.error(error);
        throw new functions.https.HttpsError('unknown', error.message);
    }
});