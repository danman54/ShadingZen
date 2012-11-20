package org.traxnet.shadingzen.core;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.microedition.khronos.opengles.GL10;

import org.traxnet.shadingzen.math.BBox;
import org.traxnet.shadingzen.math.Matrix4;
import org.traxnet.shadingzen.math.Quaternion;
import org.traxnet.shadingzen.math.Vector3;

import android.opengl.Matrix;
import android.util.Log;

public abstract class Actor extends Entity {
	protected Vector3 _position;
	protected Quaternion _rotation;
	protected float _scale;
	protected Matrix4 _worldMatrix, _localMatrix;
	protected HashMap<String, Actor> _childrenActors;
	protected Vector<Action> _activeActions;
	
	protected Shape _mesh;
	protected ShadersProgram _program;
	protected Texture _texture;
	protected Actor _parent;
	
	public Actor(){
		_rotation = new Quaternion();
		_position = new Vector3();
		_childrenActors = new HashMap<String, Actor>();
		_worldMatrix = Matrix4.identity();
		_localMatrix = Matrix4.identity();
		_activeActions = new Vector<Action>();
		_scale = 1.f;
	}
	
	@Override
	public void register(String name){
		super.register(name);
		
		_nameId = name;
	}
	
	@Override
	public void unregister(){
		for(Actor child : _childrenActors.values()){
			child.unregister();
		}
		
		_childrenActors.clear();
		
		for(Action action: _activeActions){
			
		}
		
		super.unregister();
	}
	
	/** Finds an actor given its nameId
	 * 
	 * This implementation is very slow but we don't expect a
	 * deep hierarchy
	 * 
	 * @param id nameId to look for
	 * @return An Actor or null if not found
	 */
	public Actor find(String id){
		if(_childrenActors.containsKey(id))
			return _childrenActors.get(id);
		
		for(Actor child : _childrenActors.values()){
			Actor found = child.find(id);
			if(null != found)
				return found;
		}
		
		return null;
	}
	
	public void setParent(Actor parent){
		_parent = parent;
	}
	
	public Actor getParent(){
		return _parent;
	}
	
	public void addChildren(Actor child){
		_childrenActors.put(child._nameId, child);
	}
	
	public void removeChildren(Actor child){
		//child.setParent(null);
		child.markForDestroy();
		
		for(Actor inner_child : child.getChildren()){
			child.removeChildren(inner_child);
		}
	}
	
	public void removeChildren(String childId){
		Actor child = _childrenActors.get(childId);
		//child.setParent(null);
		child.markForDestroy();
	}
	
	public Collection<Actor> getChildren(){
		return _childrenActors.values();
	}
	
	/*
	 * Called before an actor is destroyed (freed) by the engine.
	 * Derived classes MUST call super.onDestroy()
	 * 
	 * @see org.traxnet.shadingzen.core.Entity#onDestroy()
	 */
	@Override
	public void onDestroy(){
		super.onDestroy();
		
		Object [] children = this._childrenActors.values().toArray();
		
		for(Object child : children){
			removeChildren((Actor)child);
		}
		
		_activeActions.clear();
		
	}
	
	/** Get the underlying rotation quaternion 
	 * 
	 * @return A quaternon representing this shape's local rotation 
	 */
	public Quaternion getRotation(){ return _rotation; }
	
	/** Returns a totation matrix generated using the internal quaternion 
	 * @return A Matrix4 representing this shape's rotation
	 */
	public Matrix4 getRotationMatrix(){
		return _rotation.toMatrix();
	}
	
	public void setRotation(Quaternion rot){
		_rotation = rot;
	}
	public void applyRotation(Quaternion rot){
		_rotation = _rotation.mul(rot);
	}
	
	/** Get local actor position relative to parent actor */
	public Vector3 getPosition(){
		return _position;
	}
	/** Set local position relative to parent actor */
	public void setPosition(Vector3 v){
		_position = v;
	}
	
	public float getScale(){
		return _scale;
	}
	
	public void setScale(float scale){
		_scale = scale;
	}
	
	public void setWorldModelMatrix(Matrix4 matrix){
		_worldMatrix = matrix;
	}
	public Matrix4 getWorldModelMatrix(){
		return _worldMatrix;
	}
	
	public Matrix4 getLocalModelMatrix(){
		Matrix4 local_matrix;
		local_matrix = getRotationMatrix();
		local_matrix.setTranslation(getPosition());
		
		Matrix.multiplyMM(_localMatrix.getAsArray(), 0, local_matrix.getAsArray(), 0, Matrix4.scale(getScale()).getAsArray(), 0);
		return _localMatrix;
	}
	
	@Override
	public void onTick(float delta){
		runActiveActions(delta);
		
		onUpdate(delta);
	}
	
	/** Must be overriden by child classes */ 
	protected abstract void onUpdate(float deltaTime);
	
	

	// ACTIONS ////////
	
	/** Steps one delta-time for each action registered for this actor */
	private void runActiveActions(float deltaTime){
		@SuppressWarnings("unchecked")
		Vector<Action> list = (Vector<Action>) _activeActions.clone();
		for(Action action : list){
			try{
				action.step(deltaTime);
			} catch(InvalidTargetActorException e){
				Log.e("ShadingZen", "Registered action cannot be executed for the current type of actor", e);
			}
		}
	}
	
	/** Schedules the action to be executed at the next frame */
	public void runAction(Action action){
		action.setTarget(this);
		_activeActions.add(action);
	}
	
	/** Cancel and remove the given action if present */
	public void removeAction(Action action){
		if(_activeActions.contains(action)){
			_activeActions.remove(action);
			action.cancel();
		}
	}
}