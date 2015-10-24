readme file for COMP3421 assignment 2

extensions:

1. Night Mode
	features: just press n on your keyboard anytime to switch to night mode. Your
			  avatar would equip itself with a torch automatically. 
			  Press n again to switch back to day mode. The torch would disapper
			  automatically.
	where:  you can find the code for the night mode at
			keyPressed(KeyEvent) ass2.spec.Game
			setLight(GL2) ass2.spec.Game
			
2. Animated Pond
	features: you can see there is an animated pond on the terrain.
			  Unfortunately the position of the pond can not be changed. 
			  It can only be changed through the code.
	where:  you can find the code for the animated pond at		  
			setMaterialForPool(GL2) ass2.spec.Game
			drawPool(GL2) ass2.spec.Game
			also, there is the picture for the animated texture
			animationpool.jpg
			
3. Road Extrusion
	features: now the road can go up and down the hill!
			  However, to make things easier, the road still seems flat. 
			  That is, the height of the road is decided by the height of 
			  a point on the spine. So part of the road does float on the air!
	where:  you can find the code for the road extrusion at
			drawRoads() ass2.spec.Game
	