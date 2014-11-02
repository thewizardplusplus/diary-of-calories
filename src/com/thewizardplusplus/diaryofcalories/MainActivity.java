package com.thewizardplusplus.diaryofcalories;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import android.app.Activity;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.os.Environment;

public class MainActivity extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		data_accessor = DataAccessor.getInstance(this);

		label1 = (TextView)findViewById(R.id.label1);
		current_day_calories = (TextView)findViewById(
			R.id.current_day_calories
		);
		current_day_calories_unit = (TextView)findViewById(
			R.id.current_day_calories_unit
		);
		maximum_calories = (TextView)findViewById(R.id.maximum_calories);
		label3 = (TextView)findViewById(R.id.label3);
		label4 = (TextView)findViewById(R.id.label4);
		remaining_calories = (TextView)findViewById(R.id.remaining_calories);
		remaining_calories_unit = (TextView)findViewById(
			R.id.remaining_calories_unit
		);
		weight_edit = (EditText)findViewById(R.id.weight_edit);
		calories_edit = (EditText)findViewById(R.id.calories_edit);
		cancel_button = (ImageButton)findViewById(R.id.cancel_button);

		backupHistory(NotificationType.ONGOING);
		updateUi();
	}

	@Override
	public void onResume() {
		super.onResume();
		updateUi();
	}

	public void addData(View view) {
		String weight = weight_edit.getText().toString();
		String calories = calories_edit.getText().toString();
		if (weight.length() != 0 && calories.length() != 0) {
			data_accessor.addData(
				Float.parseFloat(weight),
				Float.parseFloat(calories)
			);
			backupHistory(NotificationType.HIDDING);

			updateUi();
			updateWidget();

			weight_edit.setText("");
			calories_edit.setText("");
			weight_edit.requestFocus();
		}
	}

	public void undoTheLast(View view) {
		data_accessor.undoTheLast();
		backupHistory(NotificationType.HIDDING);

		updateUi();
		updateWidget();

		weight_edit.requestFocus();
	}

	public void showHistory(View view) {
		Intent intent = new Intent(this, HistoryActivity.class);
		startActivity(intent);
	}

	public void showSettings(View view) {
		Intent intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
	}

	public void showAbout(View view) {
		Intent intent = new Intent(this, AboutActivity.class);
		startActivity(intent);
	}

	private static final String BACKUP_DIRECTORY = "diary-of-calories";
	private static final long NOTIFICATION_HIDE_DELAY = 2000;
	private static final int ONGOING_NOTIFICATION_ID = -1;

	private DataAccessor data_accessor;
	private TextView label1;
	private TextView current_day_calories;
	private TextView current_day_calories_unit;
	private TextView maximum_calories;
	private TextView label3;
	private TextView label4;
	private TextView remaining_calories;
	private TextView remaining_calories_unit;
	private EditText weight_edit;
	private EditText calories_edit;
	private ImageButton cancel_button;

	private void updateUi() {
		DayData current_day_data = data_accessor.getCurrentDayData();
		double current_day_calories = current_day_data.calories;

		label1.setText(
			String.format(getString(R.string.label1),
			Utils.convertDateToLocaleFormat(current_day_data.date))
		);
		this.current_day_calories.setText(
			Utils.convertNumberToLocaleFormat(current_day_calories)
		);

		float soft_limit = data_accessor
			.getSettings()
			.soft_limit;
		float hard_limit = data_accessor
			.getSettings()
			.hard_limit;
		double maximum_calories = 0.0;
		double difference = 0.0;
		if (current_day_calories <= hard_limit) {
			label3.setVisibility(View.VISIBLE);
			label4.setVisibility(View.GONE);

			if (current_day_calories <= soft_limit) {
				maximum_calories = soft_limit;

				this.current_day_calories.setTextColor(Color.rgb(0, 0xc0, 0));
				this.current_day_calories_unit.setTextColor(Color.rgb(0, 0xc0, 0));
				this.remaining_calories.setTextColor(Color.rgb(0, 0xc0, 0));
				this.remaining_calories_unit.setTextColor(Color.rgb(0, 0xc0, 0));
			} else {
				maximum_calories = hard_limit;

				this.current_day_calories.setTextColor(Color.rgb(0xc0, 0xc0, 0));
				this.current_day_calories_unit.setTextColor(Color.rgb(0xc0, 0xc0, 0));
				this.remaining_calories.setTextColor(Color.rgb(0xc0, 0xc0, 0));
				this.remaining_calories_unit.setTextColor(Color.rgb(0xc0, 0xc0, 0));
			}

			difference = maximum_calories - current_day_calories;
		} else {
			maximum_calories = hard_limit;
			difference = current_day_calories - maximum_calories;

			label3.setVisibility(View.GONE);
			label4.setVisibility(View.VISIBLE);

			this.current_day_calories.setTextColor(Color.rgb(0xc0, 0, 0));
			this.current_day_calories_unit.setTextColor(Color.rgb(0xc0, 0, 0));
			this.remaining_calories.setTextColor(Color.rgb(0xc0, 0, 0));
			this.remaining_calories_unit.setTextColor(Color.rgb(0xc0, 0, 0));
		}

		this.maximum_calories.setText(
			Utils.convertNumberToLocaleFormat(maximum_calories)
		);
		this.remaining_calories.setText(
			Utils.convertNumberToLocaleFormat(difference)
		);

		cancel_button.setEnabled(data_accessor.getNumberOfCurrentDayData() > 0);
	}

	private void updateWidget() {
		RemoteViews views = Widget.getUpdatedViews(this);
		ComponentName widget = new ComponentName(this, Widget.class);
		AppWidgetManager.getInstance(this).updateAppWidget(widget, views);
	}

	private void backupHistory(NotificationType notification_type) {
		String storage_state = Environment.getExternalStorageState();
		if (!Environment.MEDIA_MOUNTED.equals(storage_state)) {
			Utils.showAlertDialog(
				this,
				getString(R.string.error_message_box_title),
				getString(R.string.external_storage_error_message)
			);

			return;
		}

		File directory = new File(
			Environment.getExternalStorageDirectory(),
			BACKUP_DIRECTORY
		);
		if (!directory.exists()) {
			boolean result = directory.mkdir();
			if (!result) {
				Utils.showAlertDialog(
					this,
					getString(R.string.error_message_box_title),
					getString(R.string.directory_error_message)
				);

				return;
			}
		}

		Date current_date = new Date();
		SimpleDateFormat file_suffix_format = new SimpleDateFormat(
			"yyyy-MM-dd-HH-mm-ss"
		);
		String file_suffix = file_suffix_format.format(current_date);

		File backup_file = new File(
			directory,
			"database_dump_" + file_suffix + ".xml"
		);
		OutputStream out = null;
		try {
			try {
				out = new BufferedOutputStream(
					new FileOutputStream(backup_file)
				);
				String xml = data_accessor.getAllDataInXml();
				out.write(xml.getBytes());
			} finally {
				if (out != null) {
					out.close();
				}
			}
		} catch (IOException exception) {
			Utils.showAlertDialog(
				this,
				getString(R.string.error_message_box_title),
				getString(R.string.backup_file_error_message)
			);

			return;
		}

		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setDataAndType(Uri.fromFile(backup_file), "text/xml");
		Utils.showNotification(
			this,
			notification_type == NotificationType.ONGOING
				? ONGOING_NOTIFICATION_ID
				: 0,
			R.drawable.icon,
			getString(R.string.application_name),
			String.format(
				getString(R.string.backup_saved_notification),
				backup_file.getAbsolutePath()
			),
			intent,
			notification_type,
			NOTIFICATION_HIDE_DELAY
		);
	}
}

