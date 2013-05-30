/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: E:\\workspace\\BackgroundFileTransfer\\src\\it\\uniroma2\\backgroundfiletransfer\\aidls\\IClientService.aidl
 */
package it.uniroma2.backgroundfiletransfer.aidls;
/* The name of the remote service */
public interface IClientService extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements it.uniroma2.backgroundfiletransfer.aidls.IClientService
{
private static final java.lang.String DESCRIPTOR = "it.uniroma2.backgroundfiletransfer.aidls.IClientService";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an it.uniroma2.backgroundfiletransfer.aidls.IClientService interface,
 * generating a proxy if needed.
 */
public static it.uniroma2.backgroundfiletransfer.aidls.IClientService asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof it.uniroma2.backgroundfiletransfer.aidls.IClientService))) {
return ((it.uniroma2.backgroundfiletransfer.aidls.IClientService)iin);
}
return new it.uniroma2.backgroundfiletransfer.aidls.IClientService.Stub.Proxy(obj);
}
@Override public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_subscribe:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _arg1;
_arg1 = data.readString();
java.lang.String _result = this.subscribe(_arg0, _arg1);
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_unsubscribe:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _result = this.unsubscribe(_arg0);
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_publish:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _arg1;
_arg1 = data.readString();
java.lang.String _arg2;
_arg2 = data.readString();
java.lang.String _result = this.publish(_arg0, _arg1, _arg2);
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_unpublish:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _result = this.unpublish(_arg0);
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_get:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _arg1;
_arg1 = data.readString();
java.lang.String _result = this.get(_arg0, _arg1);
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_deleteGet:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _result = this.deleteGet(_arg0);
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_autoDownload:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _arg1;
_arg1 = data.readString();
java.lang.String _result = this.autoDownload(_arg0, _arg1);
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_notify:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String[] _result = this.notify(_arg0);
reply.writeNoException();
reply.writeStringArray(_result);
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements it.uniroma2.backgroundfiletransfer.aidls.IClientService
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
@Override public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
/* Subscribes to a specific tag, to get intents when the other devices publish sth */
@Override public java.lang.String subscribe(java.lang.String tag, java.lang.String options) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(tag);
_data.writeString(options);
mRemote.transact(Stub.TRANSACTION_subscribe, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
/* Unsubscribe to a specific tag */
@Override public java.lang.String unsubscribe(java.lang.String tag) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(tag);
mRemote.transact(Stub.TRANSACTION_unsubscribe, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
/**
	  * Publish a file that contains the digest of the files that you wish to share and the information 
	  * necessary for the other apps to recognize the files 
	  */
@Override public java.lang.String publish(java.lang.String fileLocation, java.lang.String metadata, java.lang.String options) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(fileLocation);
_data.writeString(metadata);
_data.writeString(options);
mRemote.transact(Stub.TRANSACTION_publish, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
/**
	  * Unpublish a file based on the md5digest that was returned by the publish function.
	  */
@Override public java.lang.String unpublish(java.lang.String md5digest) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(md5digest);
mRemote.transact(Stub.TRANSACTION_unpublish, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
/**
	  * Get file from other devices, knowing its md5digest.
	  */
@Override public java.lang.String get(java.lang.String md5digest, java.lang.String options) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(md5digest);
_data.writeString(options);
mRemote.transact(Stub.TRANSACTION_get, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public java.lang.String deleteGet(java.lang.String key) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(key);
mRemote.transact(Stub.TRANSACTION_deleteGet, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
/**
	  *Automatically download the result files
	  */
@Override public java.lang.String autoDownload(java.lang.String tag, java.lang.String options) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(tag);
_data.writeString(options);
mRemote.transact(Stub.TRANSACTION_autoDownload, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
/**
	  * Get notifications regarding the downloads of metadata or md5digest submitted.
	  */
@Override public java.lang.String[] notify(java.lang.String k) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String[] _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(k);
mRemote.transact(Stub.TRANSACTION_notify, _data, _reply, 0);
_reply.readException();
_result = _reply.createStringArray();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
}
static final int TRANSACTION_subscribe = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_unsubscribe = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_publish = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_unpublish = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_get = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
static final int TRANSACTION_deleteGet = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
static final int TRANSACTION_autoDownload = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
static final int TRANSACTION_notify = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
}
/* Subscribes to a specific tag, to get intents when the other devices publish sth */
public java.lang.String subscribe(java.lang.String tag, java.lang.String options) throws android.os.RemoteException;
/* Unsubscribe to a specific tag */
public java.lang.String unsubscribe(java.lang.String tag) throws android.os.RemoteException;
/**
	  * Publish a file that contains the digest of the files that you wish to share and the information 
	  * necessary for the other apps to recognize the files 
	  */
public java.lang.String publish(java.lang.String fileLocation, java.lang.String metadata, java.lang.String options) throws android.os.RemoteException;
/**
	  * Unpublish a file based on the md5digest that was returned by the publish function.
	  */
public java.lang.String unpublish(java.lang.String md5digest) throws android.os.RemoteException;
/**
	  * Get file from other devices, knowing its md5digest.
	  */
public java.lang.String get(java.lang.String md5digest, java.lang.String options) throws android.os.RemoteException;
public java.lang.String deleteGet(java.lang.String key) throws android.os.RemoteException;
/**
	  *Automatically download the result files
	  */
public java.lang.String autoDownload(java.lang.String tag, java.lang.String options) throws android.os.RemoteException;
/**
	  * Get notifications regarding the downloads of metadata or md5digest submitted.
	  */
public java.lang.String[] notify(java.lang.String k) throws android.os.RemoteException;
}
