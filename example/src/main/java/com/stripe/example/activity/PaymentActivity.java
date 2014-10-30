package com.stripe.example.activity;

import java.util.HashMap;
import java.util.Map;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;

import com.stripe.android.Stripe;
import com.stripe.android.TokenCallback;
import com.stripe.android.model.Card;
import com.stripe.android.model.Token;
import com.stripe.example.PaymentForm;
import com.stripe.example.R;
import com.stripe.example.TokenList;
import com.stripe.example.dialog.ErrorDialogFragment;
import com.stripe.example.dialog.ProgressDialogFragment;
import com.stripe.exception.APIConnectionException;
import com.stripe.exception.APIException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.model.Charge;

public class PaymentActivity extends FragmentActivity {

	/*
	 * Change this to your publishable key.
	 * 
	 * You can get your key here: https://manage.stripe.com/account/apikeys
	 */
	public static final String PUBLISHABLE_KEY = "pk_test_4Ns4Xp8GtdBUxhiUKDi4RMTa";
	public static final String SECRET_KEY = "sk_test_4Ns4tYvJt5OAT9F882yaN7cd";

	private ProgressDialogFragment progressFragment;

	Token token = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.payment_activity);

		progressFragment = ProgressDialogFragment.newInstance(R.string.progressMessage);
	}

	public void saveCreditCard(PaymentForm form) {

		Card card = new Card(form.getCardNumber(), form.getExpMonth(), form.getExpYear(), form.getCvc());

		boolean validation = card.validateCard();
		if (validation) {
			startProgress();
			new Stripe().createToken(card, PUBLISHABLE_KEY, new TokenCallback() {
				public void onSuccess(Token token) {
					PaymentActivity.this.token = token;
					getTokenList().addToList(token);
					finishProgress();
				}

				public void onError(Exception error) {
					handleError(error.getLocalizedMessage());
					finishProgress();
				}
			});
		} else if (!card.validateNumber()) {
			handleError("The card number that you entered is invalid");
		} else if (!card.validateExpiryDate()) {
			handleError("The expiration date that you entered is invalid");
		} else if (!card.validateCVC()) {
			handleError("The CVC code that you entered is invalid");
		} else {
			handleError("The card details that you entered are invalid");
		}
	}

	private void startProgress() {
		progressFragment.show(getSupportFragmentManager(), "progress");
	}

	private void finishProgress() {
		progressFragment.dismiss();
	}

	private void handleError(String error) {
		DialogFragment fragment = ErrorDialogFragment.newInstance(R.string.validationErrors, error);
		fragment.show(getSupportFragmentManager(), "error");
	}

	private TokenList getTokenList() {
		return (TokenList) (getSupportFragmentManager().findFragmentById(R.id.token_list));
	}

	public void onClickCharge(View v) {
		if (token != null) {
			startProgress();
			// Stripe.apiKey = "sk_test_BQokikJOvBiI2HlWgH4olfQ2";
			final Map<String, Object> chargeParams = new HashMap<String, Object>();
			chargeParams.put("amount", 100);
			chargeParams.put("currency", "usd");
			chargeParams.put("card", token.getId()); // obtained with Stripe.js
			chargeParams.put("description", "Charge for test@example.com");

			Map<String, String> initialMetadata = new HashMap<String, String>();
			initialMetadata.put("order_id", "6735");

			chargeParams.put("metadata", initialMetadata);

			try {

				Thread t = new Thread() {
					public void run() {

						try {
							Charge thisCharge = Charge.create(chargeParams, SECRET_KEY);
							Log.e(">>>>>", "this charge ID = " + thisCharge.getId());

						} catch (AuthenticationException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (InvalidRequestException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (APIConnectionException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (CardException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (APIException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (Exception ex) {
							System.out.println(ex.getMessage());
						}
						mHandler.post(mUpdateResults);
					}
				};
				t.start();
			} finally {

			}

		}
	}

	final Handler mHandler = new Handler();
	final Runnable mUpdateResults = new Runnable() {
		public void run() {
			finishProgress();
		}
	};
}
