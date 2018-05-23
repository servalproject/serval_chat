package org.servalproject.servalchat.peer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import org.servalproject.mid.Identity;
import org.servalproject.mid.Interface;
import org.servalproject.mid.ListObserver;
import org.servalproject.mid.Peer;
import org.servalproject.mid.Serval;
import org.servalproject.mid.networking.AbstractListObserver;
import org.servalproject.servalchat.R;
import org.servalproject.servalchat.navigation.ILifecycle;
import org.servalproject.servalchat.navigation.INavigate;
import org.servalproject.servalchat.navigation.MainActivity;
import org.servalproject.servalchat.navigation.Navigation;
import org.servalproject.servaldna.SubscriberId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jeremy on 1/05/17.
 */

public class PeerMap extends View implements INavigate, ILifecycle{
	private final Serval serval;
	private int gen=-1;
	private List<DrawNode> drawNodes = new ArrayList<>();
	private Map<SubscriberId, DrawPeer> peerMap = new HashMap<>();
	private float scale=-999;
	private float translateX=0;
	private float translateY=0;
	private Paint circlePaint;
	private Paint linePaint;
	private Paint labelPaint;
	private RectF graphBounds;
	private static final String TAG = "PeerMap";
	private GestureDetector panDetector;
	private ScaleGestureDetector scaleDetector;

	public PeerMap(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		this.serval = Serval.getInstance();
		circlePaint = new Paint();
		circlePaint.setStyle(Paint.Style.FILL);
		circlePaint.setAntiAlias(true);

		linePaint = new Paint(Color.GRAY);
		linePaint.setStyle(Paint.Style.STROKE);
		linePaint.setAntiAlias(true);

		labelPaint = new Paint(Color.LTGRAY);
		labelPaint.setTextAlign(Paint.Align.LEFT);
		labelPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		labelPaint.setUnderlineText(false);
		labelPaint.setSubpixelText(true);
		labelPaint.setAntiAlias(true);

		Self.angle = 0;
		Self.arc = (float) (Math.PI * 2);
		scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener(){
			@Override
			public boolean onScale(ScaleGestureDetector detector) {
				float scaleBy = detector.getScaleFactor();
				scale *= scaleBy;
				invalidate();
				return true;
			}
		});
		panDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener(){
			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
				translateX += distanceX / scale;
				translateY += distanceY / scale;
				invalidate();
				return true;
			}
		});
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		recalcFinalPositions();
		moveNodes();
		canvas.save();

		float xScale = (float)canvas.getWidth() / graphBounds.width();
		float yScale = (float)canvas.getHeight() / graphBounds.height();
		float idealScale = ((xScale > yScale) ? yScale : xScale) * 0.95f;
		if (idealScale > 2)
			idealScale = 2;

		// clip scale to reasonable bounds
		if (scale < idealScale)
			scale = idealScale;
		if (scale > 5)
			scale = 5;

		// clip translation to the visible edges of the graph
		float boundx = Math.max((graphBounds.width() - canvas.getWidth() / scale)/2,0);
		float boundy = Math.max((graphBounds.height() - canvas.getHeight() / scale)/2,0);
		float cx = graphBounds.centerX();
		float cy = graphBounds.centerY();
		translateX = Math.max(Math.min(translateX, cx + boundx), cx - boundx);
		translateY = Math.max(Math.min(translateY, cy + boundy), cy - boundy);

		canvas.translate(canvas.getWidth()/2 - translateX * scale, canvas.getHeight()/2 - translateY * scale);
		canvas.scale(scale, scale);

		for(DrawNode node : drawNodes) {
			if (node.visible())
				node.draw(canvas);
		}
		canvas.restore();
	}

	@Override
	public ILifecycle onAttach(MainActivity activity, Navigation n, Identity id, Peer peer, Bundle args) {
		return this;
	}

	@Override
	public void onDetach(boolean configChange) {

	}

	private void add(Peer peer){
		DrawPeer draw = new DrawPeer(peer);
		drawNodes.add(draw);
		peerMap.put(peer.getSubscriber().sid, draw);
	}

	@Override
	public void onVisible() {
		int g = serval.knownPeers.peerListObservers.add(peerObserver);
		if (g!=gen){
			drawNodes.clear();
			drawNodes.add(Self);
			for(Peer p : serval.knownPeers.getReachablePeers())
				add(p);
			gen = g;
		}
		finalPositionsDirty = true;
		invalidate();
	}

	@Override
	public void onHidden() {
		gen = serval.knownPeers.peerListObservers.remove(peerObserver);
	}

	private ListObserver<Peer> peerObserver = new AbstractListObserver<Peer>() {
		@Override
		public void added(Peer peer) {
			add(peer);
			PeerMap.this.invalidate();
		}

		@Override
		public void updated(Peer peer) {
			finalPositionsDirty = true;
			PeerMap.this.invalidate();
			//DrawPeer draw = peerMap.get(peer);
			//draw.update();
		}
	};


	private boolean finalPositionsDirty = true;

	private void recalcFinalPositions(int first, int last) {
		// fan out these nodes in an arc on a circle
		DrawNode firstNode = drawNodes.get(first);
		DrawNode parent = firstNode.getParent();
		if (parent == null)
			return;
		double baseArc = parent.arc;
		double baseAngle = parent.angle - parent.arc/2;
		int count = last - first;
		String parentLabel = parent.getLabel();
		int distance = firstNode.getDistance();
		double radius = distance * 100;
		double childSpacing = baseArc / count;
		double childArc = childSpacing;
		if (distance <4 && childArc > Math.PI*2 / Math.pow(2, distance))
			childArc = Math.PI*2 / Math.pow(2, distance);
		baseAngle += childSpacing/2;
		Log.v(TAG, parentLabel + " has "+count+" children from "+baseAngle+" + N * "+childSpacing);

		for (int i = first; i < last; i++) {
			DrawNode node = drawNodes.get(i);
			node.arc = childArc;
			node.angle = baseAngle + ((i - first) * childSpacing);
			Log.v(TAG, node.getLabel()+" " + i + " ("+(i - first)+" of " + count + ") angle " + node.angle);
			node.destX = (float) (Math.sin(node.angle) * radius);
			node.destY = (float) (Math.cos(node.angle) * radius);
		}
	}

	private void recalcFinalPositions(){
		if (!finalPositionsDirty)
			return;

		Collections.sort(drawNodes);
		Self.x = 0;
		Self.y = 0;
		Self.angle = 0;
		Self.arc = (float) (Math.PI * 2);

		if (drawNodes.size() > 1){
			int start = 1;
			int index = start+1;

			DrawNode first = drawNodes.get(start);

			for (; index < drawNodes.size(); index++){
				DrawNode node = drawNodes.get(index);
				if (node.getDistance() != first.getDistance() ||
						node.getParent() != first.getParent()) {
					recalcFinalPositions(start, index);
					start = index;
					first = node;
				}
			}

			if (start!=index)
				recalcFinalPositions(start, index);

		}

		finalPositionsDirty = false;
	}

	private void moveNodes(){
		// TODO animate each node to move towards it's destx/desty

		RectF bounds = new RectF();

		for(DrawNode n : drawNodes){
			if (n.visible()) {
				n.x = n.destX;
				n.y = n.destY;
				n.calcBounds();
				bounds.union(n.bounds);
			}
		}
		graphBounds = bounds;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		boolean scaled = scaleDetector.onTouchEvent(event);
		return panDetector.onTouchEvent(event) || scaled || super.onTouchEvent(event);
	}

	private abstract class DrawNode implements Comparable<DrawNode>{
		float destX;
		float destY;
		float x;
		float y;
		RectF bounds;

		double angle;
		double arc;

		abstract int getDistance();
		abstract int getId();
		abstract String getLabel();
		abstract int getColor();
		abstract boolean visible();

		void calcBounds(){
			bounds = new RectF();
			String label = getLabel();
			if (label!=null && !"".equals(label)) {
				Rect textBounds = new Rect();
				labelPaint.getTextBounds(label, 0, label.length(), textBounds);
				bounds.set(textBounds);
				bounds.offset(- bounds.width()/2, 12 + bounds.height());
			}
			bounds.union(-10,-10,10,10);
			bounds.offset(x, y);
		}
		void draw(Canvas canvas) {
			circlePaint.setColor(getColor());
			canvas.drawCircle(this.x, this.y, 10, circlePaint);
			DrawNode parent = getParent();
			if (parent != null)
				// see https://issuetracker.google.com/issues/36980542
				canvas.drawLines(new float[]{
						-100000f, -100000f, -100000f, -100000f,
						this.x, this.y, parent.x, parent.y}, linePaint);
			String label = getLabel();
			if (label!=null && !"".equals(label)){
				Rect dest =  new Rect();
				labelPaint.getTextBounds(label, 0, label.length(), dest);
				canvas.drawText(label, 0, label.length(),
						this.x - dest.width()/2, this.y + 12 + dest.height(), labelPaint);
			}
		}

		abstract DrawNode getParent();

		@Override
		public int compareTo(@NonNull DrawNode drawNode) {
			if (this == drawNode)
				return 0;

			int distance = getDistance();
			int theirDistance = drawNode.getDistance();
			if (distance != theirDistance)
				return (distance < theirDistance) ? -1 : 1;

			DrawNode myParent = getParent();
			DrawNode theirParent = drawNode.getParent();
			if (myParent != theirParent){
				if (myParent==null)
					return -1;
				if (theirParent==null)
					return 1;
				return myParent.compareTo(theirParent);
			}

			return getId() < drawNode.getId() ? -1 : 1;
		}
	}

	private DrawNode Self = new DrawNode() {
		@Override
		int getDistance() {
			return 0;
		}

		@Override
		int getId() {
			return 0;
		}

		@Override
		String getLabel() {
			return PeerMap.this.getResources().getString(R.string.map_self);
		}

		@Override
		int getColor() {
			return Color.BLACK;
		}

		@Override
		boolean visible() {
			return true;
		}

		@Override
		DrawNode getParent() {
			return null;
		}
	};

	private class DrawPeer extends DrawNode{
		final Peer peer;
		private int hopCount;

		DrawPeer(Peer peer){
			this.peer = peer;
		}

		@Override
		int getDistance() {
			int newHopCount = peer.getHopCount();
			if (newHopCount >=1 )
				hopCount = newHopCount;
			return hopCount;
		}

		@Override
		int getId() {
			return (int) peer.getId();
		}

		@Override
		String getLabel() {
			return peer.displayName();
		}

		@Override
		int getColor() {
			Interface netInterface = peer.getNetInterface();
			if (netInterface!=null){
				if (netInterface.name.equals("bluetooth"))
					return Color.BLUE;
				return Color.GREEN;
			}
			return Color.LTGRAY;
		}

		@Override
		boolean visible() {
			return peer.getHopCount()>=1;
		}

		@Override
		DrawNode getParent() {
			Peer priorHop = peer.getPriorHop();
			if (priorHop==null)
				return Self;
			return peerMap.get(priorHop.getSubscriber().sid);
		}
	}
}
