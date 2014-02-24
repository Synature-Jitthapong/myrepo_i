package com.syn.iorder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import syn.pos.data.dao.AppConfig;
import syn.pos.data.dao.ComputerProperty;
import syn.pos.data.dao.GlobalProperty;
import syn.pos.data.dao.MenuComment;
import syn.pos.data.dao.MenuDept;
import syn.pos.data.dao.MenuGroup;
import syn.pos.data.dao.MenuItem;
import syn.pos.data.dao.PComponentGroup;
import syn.pos.data.dao.PComponentSet;
import syn.pos.data.dao.POSOrdering;
import syn.pos.data.dao.Product;
import syn.pos.data.dao.ProductComponent;
import syn.pos.data.dao.ProductDept;
import syn.pos.data.dao.ProductGroup;
import syn.pos.data.dao.QuestionGroups;
import syn.pos.data.dao.Reason;
import syn.pos.data.dao.Register;
import syn.pos.data.dao.SaleMode;
import syn.pos.data.dao.ShopProperty;
import syn.pos.data.dao.ProgramFeature;
import syn.pos.data.dao.SyncDataLog;
import syn.pos.data.dao.TransactionComment;
import syn.pos.data.json.GsonDeserialze;
import syn.pos.data.model.MenuGroups;
import syn.pos.data.model.OrderSendData;
import syn.pos.data.model.ProductGroups;
import syn.pos.data.model.QueueInfo;
import syn.pos.data.model.ReasonGroups;
import syn.pos.data.model.ShopData;
import syn.pos.data.model.SyncDataLogModel;
import syn.pos.data.model.TableInfo;
import syn.pos.data.model.TableName;
import syn.pos.data.model.WebServiceResult;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class IOrderUtility {
	private static ProgressDialog progress;
	private WebServiceStateListener webServiceStateListener;
	
	public static class CompareSaleDateTask extends SaleDateTask{
		private WebServiceStateListener webServiceState;
		public CompareSaleDateTask(Context c, GlobalVar gb, WebServiceStateListener state) {
			super(c, gb);
			
			webServiceState = state;
			IOrderUtility.progress = new ProgressDialog(c);
			tvProgress.setText(R.string.loading_progress);
			IOrderUtility.progress.setMessage(tvProgress.getText());
		}
		
		@Override
		protected void onPreExecute() {
			if(progress.isShowing())
				progress.dismiss();
			
			IOrderUtility.progress.show();
		}
		
		@Override
		protected void onPostExecute(String result) {
			if(IOrderUtility.progress.isShowing())
				IOrderUtility.progress.dismiss();
			
			GsonDeserialze gdz = new GsonDeserialze();
			String strSaleDate = "";
			try {
				WebServiceResult wsResult = gdz.deserializeWsResultJSON(result);
				strSaleDate = wsResult.getSzResultData();
				
				// check sync log
				SyncDataLog sync = new SyncDataLog(context);
				SyncDataLogModel syncData = sync.getSyncTimeStamp();
				
				if (syncData.getSyncDate() != null) {
					try {

						Date currDate = new Date();
						
						Date logDate = new SimpleDateFormat("yyyy-MM-dd",
								Locale.US).parse(syncData.getSyncDate());
						Date saleDate = new SimpleDateFormat("yyyy-MM-dd",
								Locale.US).parse(strSaleDate);

						if(currDate.compareTo(saleDate) > 0){
							if (logDate.compareTo(saleDate) == 0) {
								sync.setCompareDateFail(1);
								webServiceState.onSuccess();
							}else{
								sync.setCompareDateFail(0);
								webServiceState.onNotSuccess();
							}
						}else{
							webServiceState.onSuccess();
						}
					} catch (ParseException e) {
						webServiceState.onNotSuccess();
					}
				}else{
					webServiceState.onNotSuccess();
				}
				
			} catch (Exception e) {
				IOrderUtility.alertDialog(context, R.string.global_dialog_title_error, result, 0);
			}
		}
	}
	
	public static List<ProductGroups.PComponentSet> listPComSetOfProduct(Context c, int productId){
		PComponentSet pCompSet = new PComponentSet(c);
		return pCompSet.listPComponentSetOfProduct(productId);
	}
	
	public static List<ProductGroups.PComponentSet> listPComSet(Context c, int productId){
		PComponentSet pCompSet = new PComponentSet(c);
		return pCompSet.listPComponentSize(productId);
	}
	
	public static ProductSizeAdapter getProductSize(Context c, int productId) {
		List<ProductGroups.PComponentSet> sizeList = listPComSet(c, productId);
		ProductSizeAdapter adapter = new ProductSizeAdapter(c, sizeList);
		return adapter;
	}

	public static void alertDialog(Context context, int title, String message,
			int btnStyle) {
		final CustomDialog customDialog = new CustomDialog(context,
				R.style.CustomDialog);
		customDialog.title.setVisibility(View.VISIBLE);
		customDialog.title.setText(title);
		customDialog.message.setText(message);
		customDialog.btnCancel.setVisibility(View.GONE);
		customDialog.btnOk.setText(R.string.global_close_dialog_btn);
//		customDialog.btnOk.setBackgroundResource(btnStyle != 0 ? btnStyle
//				: R.drawable.red_button);
		customDialog.btnOk.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				customDialog.dismiss();
			}
		});
		customDialog.show();
	}

	public static void alertDialog(Context context, int message,
			int btnStyle) {
		final CustomDialog customDialog = new CustomDialog(context,
				R.style.CustomDialog);
		customDialog.message.setText(message);
		customDialog.btnCancel.setVisibility(View.GONE);
		customDialog.btnOk.setText(R.string.global_close_dialog_btn);
//		customDialog.btnOk.setBackgroundResource(btnStyle != 0 ? btnStyle
//				: R.drawable.red_button);
		customDialog.btnOk.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				customDialog.dismiss();
			}
		});
		customDialog.show();
	}
	
	public static void alertDialog(Context context, int title, int message,
			int btnStyle) {
		final CustomDialog customDialog = new CustomDialog(context,
				R.style.CustomDialog);
		customDialog.title.setVisibility(View.VISIBLE);
		customDialog.title.setText(title);
		customDialog.message.setText(message);
		customDialog.btnCancel.setVisibility(View.GONE);
		customDialog.btnOk.setText(R.string.global_close_dialog_btn);
//		customDialog.btnOk.setBackgroundResource(btnStyle != 0 ? btnStyle
//				: R.drawable.red_button);
		customDialog.btnOk.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				customDialog.dismiss();
			}
		});
		customDialog.show();
	}

	public static List<ReasonGroups.ReasonDetail> loadReasonFromWs(Context c,
			GlobalVar globalVar, int reasonGroupId) {
		List<ReasonGroups.ReasonDetail> reasonDetailLst = new ArrayList<ReasonGroups.ReasonDetail>();
		Reason reason = new Reason(c);
		reasonDetailLst = reason.listAllReasonDetail(reasonGroupId);
		return reasonDetailLst;
	}

	public void syncDataWebService(Context context, GlobalVar globalVar) {
		// delete log
		deleteLog(context);
		POSOrdering posOrder = new POSOrdering(context);
		posOrder.clearTransaction();

		new CheckAuthenShopFromWs(context, globalVar)
				.execute(GlobalVar.FULL_URL);
	}
	
	public void syncDataWebService(Context context, GlobalVar globalVar, WebServiceStateListener serviceState) {
		// delete log
		deleteLog(context);
		POSOrdering posOrder = new POSOrdering(context);
		posOrder.clearTransaction();

		this.webServiceStateListener = serviceState;
		
		new CheckAuthenShopFromWs(context, globalVar)
				.execute(GlobalVar.FULL_URL);
	}
	
	public static TableZoneSpinnerAdapter createTableZoneAdapter(
			Context context, TableName tbName) {
		if (tbName != null) {
			TableName.TableZone tbZone = new TableName.TableZone();
			tbZone.setZoneID(0);
			tbZone.setZoneName("All Zone");
			tbName.TableZone.add(0, tbZone);

			TableZoneSpinnerAdapter tbZoneAdapter = 
					new TableZoneSpinnerAdapter(context, tbName.TableZone);

			return tbZoneAdapter;
		}
		return null;
	}

	public static SelectTableListAdapter createTableNameAdapter(
			Context context, GlobalVar globalVar,
			final List<TableInfo> tbInfoLst, boolean isShowCapacity, boolean isShowBtnInfo) {
		SelectTableListAdapter tbNameAdapter = new SelectTableListAdapter(
				context, globalVar, tbInfoLst, isShowCapacity, isShowBtnInfo);
		return tbNameAdapter;
	}

	public static int indexOfQueueList(List<QueueInfo> queueLst, int queueId) {
		int index = -1;
		for (int i = 0; i < queueLst.size(); i++) {
			if (queueId == queueLst.get(i).getiQueueID()) {
				index = i;
				break;
			}
		}
		return index;
	}

	public static int indexOfTbList(List<TableInfo> tbInfoLst,
			int tableId) {
		int index = -1;
		for (int i = 0; i < tbInfoLst.size(); i++) {
			if (tableId == tbInfoLst.get(i).getiTableID()) {
				index = i;
				break;
			}
		}
		return index;
	}

	public static List<TableInfo> filterTableNameHaveOrder(List<TableInfo> tbInfoLst,
			TableName.TableZone tbZone) {
		List<TableInfo> tbFilterLst = new ArrayList<TableInfo>();
		for (TableInfo tbInfo : tbInfoLst) {
			if (tbInfo.isbHasOrder()) {
				if (tbZone.getZoneID() != 0) {
					if (tbZone.getZoneID() == tbInfo.getiTableZoneID()) {
						tbFilterLst.add(tbInfo);
					}
				} else {
					tbFilterLst.add(tbInfo);
				}
			}
		}
		return tbFilterLst;
	}

	public static List<TableInfo> filterTableName(List<TableInfo> tbInfoLst,
			TableName.TableZone tbZone, int tableId) {
		List<TableInfo> tbFilterLst = new ArrayList<TableInfo>();
		for (TableInfo tbInfo : tbInfoLst) {
			if (tbInfo.getiTableID() != tableId && !tbInfo.isbIsCombineTable()) {
				if (tbZone.getZoneID() != 0) {
					if (tbZone.getZoneID() == tbInfo.getiTableZoneID()) {
						tbFilterLst.add(tbInfo);
					}
				} else {
					tbFilterLst.add(tbInfo);
				}
			}
		}
		return tbFilterLst;
	}

	public static List<TableInfo> filterEmptyTableName(List<TableInfo> tbInfoLst,
			TableName.TableZone tbZone) {
		List<TableInfo> tbFilterLst = new ArrayList<TableInfo>();
		for (TableInfo tbInfo : tbInfoLst) {
			if (tbInfo.getTableStatus() == 0) {
				if (tbZone.getZoneID() != 0) {
					if (tbZone.getZoneID() == tbInfo.getiTableZoneID()) {
						tbFilterLst.add(tbInfo);
					}
				} else {
					tbFilterLst.add(tbInfo);
				}
			}
		}
		return tbFilterLst;
	}

	public static List<OrderSendData.OrderDetail> filterProductType(List<OrderSendData.OrderDetail> order){
		List<OrderSendData.OrderDetail> orderFilter = 
				new ArrayList<OrderSendData.OrderDetail>();
		for(OrderSendData.OrderDetail detail : order){
			if(detail.getiProductType() != 14 && detail.getiProductType() != 15){
				orderFilter.add(detail);
			}
		}
		return orderFilter;
	}
	
	public static List<TableInfo> filterTableName(List<TableInfo> tbInfoLst,
			TableName.TableZone tbZone) {
		List<TableInfo> tbFilterLst = new ArrayList<TableInfo>();
		for (TableInfo tbInfo : tbInfoLst) {
			if (tbZone.getZoneID() != 0) {
				if (tbZone.getZoneID() == tbInfo.getiTableZoneID()) {
					tbFilterLst.add(tbInfo);
				}
			} else {
				tbFilterLst.add(tbInfo);
			}
		}
		return tbFilterLst;
	}

	// Web service zone
	// Check authen shop
	public class CheckAuthenShopFromWs extends WebServiceTask {
		private static final String webMethod = "WSmPOS_CheckAuthenShopDevice";
		private Context context;

		public CheckAuthenShopFromWs(Context c, GlobalVar gb) {
			super(c, gb, webMethod);
			context = c;
			IOrderUtility.progress = new ProgressDialog(c);
			IOrderUtility.progress.setCanceledOnTouchOutside(false);
			// DataBaseHelper dbHelper = new DataBaseHelper(c);
			// dbHelper.deleteDbFile();
		}

		@Override
		protected void onPostExecute(String result) {
			if (progress.isShowing())
				progress.dismiss();

			int shopId = 0;
			try {
				shopId = Integer.parseInt(result); // -1 : computer set not
													// valid
				if (shopId > 0) {
					GlobalVar.SHOP_ID = shopId;

					new UpdateShopDataFromWs(context, globalVar)
							.execute(GlobalVar.FULL_URL);
				} else if (shopId == -1) {
					IOrderUtility.alertDialog(context,
							R.string.global_dialog_title_error,
							R.string.text_comp_setting_not_valid, 0);
					IOrderUtility.progress.dismiss();
				} else {
					IOrderUtility.alertDialog(context,
							R.string.global_dialog_title_error,
							R.string.device_not_register, 0);
					IOrderUtility.progress.dismiss();
				}
			} catch (NumberFormatException e) {
				IOrderUtility.progress.dismiss();
				IOrderUtility.alertDialog(context,
						R.string.global_dialog_title_error, result, 0);
			}
		}

		@Override
		protected void onPreExecute() {
			tvProgress.setText(R.string.checking_device);
			IOrderUtility.progress.setMessage(tvProgress.getText());
			IOrderUtility.progress.show();
			// progress.setMessage(tvProgress.getText().toString());
			// progress.show();
			progress.dismiss();
		}
	}

	public class UpdateShopDataFromWs extends WebServiceTask {

		private final static String webMethod = "WSmPOS_JSON_LoadShopData";

		public UpdateShopDataFromWs(Context c, GlobalVar gb) {
			super(c, gb, webMethod);
		}

		@Override
		protected void onPreExecute() {
			progress.dismiss();
			tvProgress.setText(R.string.update_shop_progress);
			IOrderUtility.progress.setMessage(tvProgress.getText());
		}

		@Override
		protected void onPostExecute(String result) {
			if (progress.isShowing())
				progress.dismiss();

			GsonDeserialze gdz = new GsonDeserialze();

			try {				
				ShopData sd = gdz.deserializeShopDataJSON(result);
				ShopProperty sp = new ShopProperty(context, null);
				sp.insertShopProperty(sd);
				sp.insertStaffData(sd);
				sp.insertLanguage(sd);
				
				try {
					sp.insertStaffPermissionData(sd);
					sp.insertSeatNo(sd);
					sp.insertCourse(sd);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					
					IOrderUtility.alertDialog(context, R.string.global_dialog_title_error, e.getMessage(), 0);
				}

				ComputerProperty cp = new ComputerProperty(context);
				cp.insertComputerProperty(sd);

				GlobalProperty gb = new GlobalProperty(context, null);
				gb.insertGlobalProperty(sd);

				ProgramFeature pf = new ProgramFeature(context);
				pf.insertProgramFeature(sd);
				
				new UpdateProductDataFromWs(context, globalVar)
						.execute(GlobalVar.FULL_URL);
			} catch (Exception e) {
				IOrderUtility.progress.dismiss();
				syn.pos.mobile.util.Log.appendLog(context,
						"Update shop data fail => " + result);

				final CustomDialog customDialog = new CustomDialog(context,
						R.style.CustomDialog);
				customDialog.title.setVisibility(View.VISIBLE);
				customDialog.title.setText(R.string.global_dialog_title_error);
				customDialog.message.setText(result);
				customDialog.btnCancel.setVisibility(View.GONE);
				customDialog.btnOk.setText(R.string.global_close_dialog_btn);
				customDialog.btnOk.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						customDialog.dismiss();
					}
				});
				customDialog.show();
			}
		}
	}

	public class UpdateProductDataFromWs extends WebServiceTask {
		private final static String webMethod = "WSmPOS_JSON_LoadProductDataV2";

		public UpdateProductDataFromWs(Context c, GlobalVar gb) {
			super(c, gb, webMethod);

			tvProgress.setText(R.string.update_product_progress);
		}

		@Override
		protected void onPreExecute() {
			progress.dismiss();
			IOrderUtility.progress.setMessage(tvProgress.getText());
		}

		@Override
		protected void onPostExecute(String result) {
			if (progress.isShowing())
				progress.dismiss();

			GsonDeserialze gdz = new GsonDeserialze();

			try {
				gdz = new GsonDeserialze();
				ProductGroups pgs = gdz.deserializeProductGroupJSON(result);
				Product p = new Product(context);
				p.insertProduct(pgs);
				p.insertCommentProduct(pgs);

				ProductGroup pg = new ProductGroup(context);
				pg.insertProductGroup(pgs);

				ProductDept pd = new ProductDept(context);
				pd.insertProductDept(pgs);

				PComponentGroup pcg = new PComponentGroup(context);
				pcg.insertPcomponentGroup(pgs);

				PComponentSet pcs = new PComponentSet(context);
				pcs.insertPcomponentSet(pgs);

				ProductComponent pc = new ProductComponent(context);
				pc.insertProductComponent(pgs);
				
				SaleMode s = new SaleMode(context);
				s.insertSaleMode(pgs);
				
				QuestionGroups qg = new QuestionGroups(context);
				qg.insertQuestionGroups(pgs);
				qg.insertQuestionDetail(pgs);
				qg.insertAnswerOption(pgs);
				
				try {
					TransactionComment tc = new TransactionComment(context);
					tc.insertCommentTransDept(pgs);
					tc.insertCommentTransItem(pgs);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				new UpdateMenuDataFromWs(context, globalVar)
						.execute(GlobalVar.FULL_URL);
			} catch (Exception e) {
				IOrderUtility.progress.dismiss();
				syn.pos.mobile.util.Log.appendLog(context,
						"Update product data fail => " + e.getMessage());

				final CustomDialog customDialog = new CustomDialog(context,
						R.style.CustomDialog);
				customDialog.title.setVisibility(View.VISIBLE);
				customDialog.title.setText(R.string.global_dialog_title_error);
				customDialog.message.setText(result);
				customDialog.btnCancel.setVisibility(View.GONE);
				customDialog.btnOk.setText(R.string.global_close_dialog_btn);
				customDialog.btnOk.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						customDialog.dismiss();
					}
				});
				customDialog.show();
			}
		}
	}

	public class UpdateMenuDataFromWs extends WebServiceTask {
		private static final String webMethod = "WSmPOS_JSON_LoadMenuDataV2";

		public UpdateMenuDataFromWs(Context c, GlobalVar gb) {
			super(c, gb, webMethod);

			tvProgress.setText(R.string.update_menu_progress);
		}

		@Override
		protected void onPreExecute() {
			progress.dismiss();
			IOrderUtility.progress.setMessage(tvProgress.getText());
		}

		@Override
		protected void onPostExecute(String result) {
			if (progress.isShowing())
				progress.dismiss();

			GsonDeserialze gdz = new GsonDeserialze();

			try {
				gdz = new GsonDeserialze();
				MenuGroups mgs = gdz.deserializeMenuGroupsJSON(result);
				MenuGroup mg = new MenuGroup(context);
				mg.insertMenuGroup(mgs);

				MenuDept md = new MenuDept(context);
				md.insertMenuDept(mgs);

				MenuItem mi = new MenuItem(context);
				mi.insertMenuItem(mgs);

				MenuComment mc = new MenuComment(context);
				mc.insertMenuCommentGroup(mgs);
				mc.insertMenuComment2(mgs);
				mc.insertMenuFixComment(mgs);

				FileCache fileCache = new FileCache(context);
				fileCache.clear();

				new UpdateReasonDataFromWs(context, globalVar)
						.execute(GlobalVar.FULL_URL);
			} catch (Exception e) {
				IOrderUtility.progress.dismiss();
				syn.pos.mobile.util.Log.appendLog(context,
						"Update menu data fail => " + result);

				final CustomDialog customDialog = new CustomDialog(context,
						R.style.CustomDialog);
				customDialog.title.setVisibility(View.VISIBLE);
				customDialog.title.setText(R.string.global_dialog_title_error);
				customDialog.message.setText(result);
				customDialog.btnCancel.setVisibility(View.GONE);
				customDialog.btnOk.setText(R.string.global_close_dialog_btn);
				customDialog.btnOk.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						customDialog.dismiss();
					}
				});
				customDialog.show();
			}
		}
	}

	public class UpdateReasonDataFromWs extends WebServiceTask {
		private static final String webMethod = "WSmPOS_JSON_LoadReasonData";

		public UpdateReasonDataFromWs(Context c, GlobalVar gb) {
			super(c, gb, webMethod);

			//tvProgress.setText(R.string.update_reason_progress);
		}

		@Override
		protected void onPreExecute() {
			progress.dismiss();
			//IOrderUtility.progress.setMessage(tvProgress.getText());
		}

		@Override
		protected void onPostExecute(String result) {
			if (progress.isShowing())
				progress.dismiss();

			GsonDeserialze gdz = new GsonDeserialze();

			try {
				ReasonGroups reasonGroup;
				reasonGroup = gdz.deserializeReasonGroupJSON(result);
				Reason reason = new Reason(context);
				reason.addReasonGroup(reasonGroup);
				reason.addReasonDetail(reasonGroup);
				
				new UpdateSaleDateTask(context, globalVar).execute(GlobalVar.FULL_URL);
			} catch (Exception e) {
				IOrderUtility.progress.dismiss();
				syn.pos.mobile.util.Log.appendLog(context,
						"Update reason data fail => " + result);

				final CustomDialog customDialog = new CustomDialog(context,
						R.style.CustomDialog);
				customDialog.title.setVisibility(View.VISIBLE);
				customDialog.title.setText(R.string.global_dialog_title_error);
				customDialog.message.setText(result);
				customDialog.btnCancel.setVisibility(View.GONE);
				customDialog.btnOk.setText(R.string.global_close_dialog_btn);
				customDialog.btnOk.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						customDialog.dismiss();
					}
				});
				customDialog.show();
			}
		}
	}

	public static class UpdateWebServiceVersion extends WebServiceTask{
		private static final String method = "WS_JSON_GetVersionInfo";
		
		private WebServiceStateListener stateListener;
		public UpdateWebServiceVersion(Context c, GlobalVar gb, WebServiceStateListener state) {
			super(c, gb, method);
			stateListener = state;
		}

		@Override
		protected void onPreExecute() {
			if(progress.isShowing())
				progress.dismiss();
		}

		@Override
		protected void onPostExecute(String result) {
			GsonDeserialze gdz = new GsonDeserialze();
			try {
				WebServiceResult.WebServiceVersion version = 
						gdz.deserializeWebServiceVersion(result);
				SyncDataLog syncLog = new SyncDataLog(context);
				syncLog.updateWebServiceVersion(version);
				stateListener.onSuccess();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				stateListener.onNotSuccess();
				e.printStackTrace();
			}
		}
	}
	
	public class UpdateSaleDateTask extends SaleDateTask{
		public UpdateSaleDateTask(Context c, GlobalVar gb) {
			super(c, gb);
		}
		
		@Override
		protected void onPreExecute() {
			progress.dismiss();
		}
		@Override
		protected void onPostExecute(String result) {
			if (progress.isShowing())
				progress.dismiss();

			if (IOrderUtility.progress.isShowing())
				IOrderUtility.progress.dismiss();
			
			GsonDeserialze gdz = new GsonDeserialze();
			
			try {
				WebServiceResult wsResult = gdz.deserializeWsResultJSON(result);
				
				SyncDataLog syncLog = new SyncDataLog(context);
				syncLog.stampTime(1, "", wsResult.getSzResultData());
				
				// already success
				final CustomDialog customDialog = new CustomDialog(context,
						R.style.CustomDialog);
				customDialog.title.setVisibility(View.VISIBLE);
				customDialog.title.setText(R.string.update_data_title);
				customDialog.message.setText(R.string.sync_data_success);
				customDialog.btnCancel.setVisibility(View.GONE);
				customDialog.btnOk.setText(R.string.global_close_dialog_btn);
//				customDialog.btnOk
//						.setBackgroundResource(R.drawable.green_button);
				customDialog.btnOk.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						// call back 
						try {
							
							// update out of product
							new CheckOutOfProductTask(context,
									globalVar).execute(GlobalVar.FULL_URL);
							
							webServiceStateListener.onSuccess();
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						customDialog.dismiss();
					}
				});
				customDialog.show();
				
			} catch (Exception e) {
				IOrderUtility.alertDialog(context, R.string.global_dialog_title_error, result, 0);
			}
		}
		
	}
	
	public static void touchQty(final GlobalVar gb, Button bMinus,
			Button bPlus, final TextView tvQty) {
		bMinus.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				int qty = Integer.parseInt(tvQty.getText().toString());
				if (--qty > 0)
					tvQty.setText(gb.qtyFormat.format(qty));
			}

		});

		bPlus.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				int qty = Integer.parseInt(tvQty.getText().toString());
				tvQty.setText(gb.qtyFormat.format(++qty));
			}

		});
	}

//	public static class LoadTableTask extends WebServiceTask {
//
//		private Spinner spTbZone;
//		private ListView lvTbName;
//		private LinearLayout layoutProgress;
//
//		public LoadTableTask(Context c, GlobalVar gb, Spinner spTableZone,
//				ListView lvTableName, LinearLayout layoutPrgs) {
//			super(c, gb, "WSmPOS_JSON_LoadAllTableDataV2");
//			spTbZone = spTableZone;
//			lvTbName = lvTableName;
//			layoutProgress = layoutPrgs;
//		}
//
//		@Override
//		protected void onPreExecute() {
//			layoutProgress.setVisibility(View.VISIBLE);
//		}
//
//		@Override
//		protected void onPostExecute(String result) {
//			layoutProgress.setVisibility(View.GONE);
//
//			GsonDeserialze gdz = new GsonDeserialze();
//			try {
//				final TableInfo tbInfo = gdz.deserializeTableInfoJSON(result);
//				spTbZone.setAdapter(IOrderUtility.createTableZoneAdapter(
//						context, tbInfo));
//				spTbZone.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
//
//					@Override
//					public void onItemSelected(AdapterView<?> parent, View v,
//							int pos, long id) {
//						TableZone tbZone = (TableZone) parent
//								.getItemAtPosition(pos);
//
//						List<TableName> tbNameList = IOrderUtility
//								.filterTableName(tbInfo, tbZone);
//
//						SelectTableListAdapter adapter = new SelectTableListAdapter(
//								context, globalVar, tbNameList, false);
//
//						lvTbName.setAdapter(adapter);
//					}
//
//					@Override
//					public void onNothingSelected(AdapterView<?> arg0) {
//						// TODO Auto-generated method stub
//
//					}
//				});
//			} catch (Exception e) {
//				IOrderUtility.alertDialog(context,
//						R.string.global_dialog_title_error, result, 0);
//			}
//		}
//
//	}

	public static class CheckOutOfProductTask extends WebServiceTask {
		
		private OutOfStockUpdate mCallback;
		
		public CheckOutOfProductTask(Context c, GlobalVar gb) {
			super(c, gb, "WSiOrder_JSON_GetOutOfProduct");
		}
		
		public CheckOutOfProductTask(Context c, GlobalVar gb, OutOfStockUpdate callback) {
			super(c, gb, "WSiOrder_JSON_GetOutOfProduct");

			mCallback = callback;
		}

		@Override
		protected void onPreExecute() {
			progress.dismiss();
		}

		@Override
		protected void onPostExecute(String result) {
			// result as list of integer
			Gson gson = new Gson();
			Type type = new TypeToken<List<Integer>>() {
			}.getType();
			try {
				List<Integer> proIdLst = gson.fromJson(result, type);
				try {
					Product p = new Product(context);
					p.setOutOfStock(proIdLst);
					mCallback.onUpdated();
				} catch (Exception e) {
					e.printStackTrace();
				}
			} catch (JsonSyntaxException e) {
				e.printStackTrace();
			}
		}
		
		public static interface OutOfStockUpdate{
			void onUpdated();
		}
	}

	public static void deleteLog(Context context) {
		File sdcard = Environment.getExternalStorageDirectory();

		File file = new File(sdcard, context.getPackageName());

		if (file != null) {
			File[] files = file.listFiles();
			for(File f : files){
				if(f.toString().contains(".txt")){
					try {
						f.delete();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	public static void readLog(Context context, TextView tv) {
		File sdcard = Environment.getExternalStorageDirectory();

		File file = new File(sdcard, context.getPackageName() + "_log.txt");

		StringBuilder text = new StringBuilder();

		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;

			while ((line = br.readLine()) != null) {
				text.append(line);
				text.append('\n');
			}
		} catch (IOException e) {
			// You'll need to add proper error handling here
		}

		tv.setText(text);
	}
	
	public static boolean checkRegister(Context c){
		Register register = new Register(c);
		register.getRegisterInfo();
		if (register.getRegisterCode() != null
				&& !register.getRegisterCode().equals("")) {

			try {
				int checkProCode = -1;
				checkProCode = com.syn.iorder.util.SynRegisterAlghorhythm
						.checkProductCode(register.getKeyCode());
				if (checkProCode == 0) {
					try {
						int compare = -1;
						compare = com.syn.iorder.util.SynRegisterAlghorhythm
								.comparison(register.getKeyCode(),
										register.getDeviceCode(),
										register.getRegisterCode());
						if (compare == 0) {
							return true;
						} 
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {

				}

			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		}
		return false;
	}
	
	public static String formatJSONDate(GlobalVar globalVar, String tbTime){
		String reg = "[^0-9]";
		String timeStr = "";
		try {
			long tbTimeMillisec = 0;
			/* split "+0700" from dot net webservice
			 * because never use it
			 */
			String[] filterPlus7 = tbTime.split("\\+");
			if(filterPlus7.length > 1)
				tbTimeMillisec = Long.parseLong(filterPlus7[0].replaceAll(reg, ""));
			else
				tbTimeMillisec = Long.parseLong(tbTime.replaceAll(reg, ""));
			
			Calendar c = Calendar.getInstance();
			c.setTimeInMillis(tbTimeMillisec);
			timeStr = globalVar.mTimeFormat.format(c.getTime());
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return timeStr;
	}
	
	public static double stringToDouble(String text){
		double value = 0.0d;
		NumberFormat format = NumberFormat.getInstance(Locale.getDefault());
		Number num = value;
		try {
			num = format.parse(text);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		value = num.doubleValue();
		return value;
	}
	
	public static String formatCombindTableName(boolean isCombind, String combindTbName, String currTbName){
		if(!isCombind)
			return currTbName;
		
		String tbName = null;
		if(GlobalVar.sIsEnableMaxCharFormergeTable){
			tbName = currTbName + " [" + combindTbName + "]";
		}else{
			tbName = currTbName + "+" + combindTbName;
		}
		return tbName;
	}
	
	public static boolean checkConfig(Context c){
		AppConfig setting = new AppConfig(c);
		if(setting.getConfig().getServerIP() != null &&
				!setting.getConfig().getServerIP().equals(""))
			return true;
		return false;
	}
	
	public static void switchLanguage(Context context, int langId){
		Resources res = context.getResources();
	    // Change locale settings in the app.
	    DisplayMetrics dm = res.getDisplayMetrics();
	    android.content.res.Configuration conf = res.getConfiguration();
	    if(langId == 1)
	    	conf.locale = Locale.US;
	    else if(langId == 2)
	    	conf.locale = new Locale("th");
	    res.updateConfiguration(conf, dm);
	}
}
