package com.firebase.ui.storage.images;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.ModelLoader.LoadData;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.InputStream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class StringLoaderTest {

    private static final String VALID_GS_URL = "gs://my-bucket.appspot.com/images/photo.png";
    private static final String INVALID_GS_URL = "gs://bad-url";

    @Mock FirebaseStorage mStorage;
    @Mock StorageReference mRef;

    @Before
    public void setUp() {
        when(mStorage.getReferenceFromUrl(VALID_GS_URL)).thenReturn(mRef);
    }

    // handles()

    @Test
    public void handles_gsUrl_returnsTrue() {
        assertTrue(new FirebaseImageLoader.StringLoader(mStorage).handles(VALID_GS_URL));
    }

    @Test
    public void handles_httpsUrl_returnsFalse() {
        assertFalse(new FirebaseImageLoader.StringLoader(mStorage).handles("https://example.com/image.png"));
    }

    @Test
    public void handles_emptyString_returnsFalse() {
        assertFalse(new FirebaseImageLoader.StringLoader(mStorage).handles(""));
    }

    // buildLoadData()

    @Test
    public void buildLoadData_validUrl_returnsLoadData() {
        LoadData<InputStream> result = loaderWithMockStorage().buildLoadData(VALID_GS_URL, 0, 0, new Options());
        assertNotNull(result);
    }

    @Test
    public void buildLoadData_illegalArgumentException_returnsNull() {
        when(mStorage.getReferenceFromUrl(INVALID_GS_URL)).thenThrow(new IllegalArgumentException());
        LoadData<InputStream> result = loaderWithMockStorage().buildLoadData(INVALID_GS_URL, 0, 0, new Options());
        assertNull(result);
    }

    @Test
    public void buildLoadData_illegalStateException_returnsNull() {
        when(mStorage.getReferenceFromUrl(VALID_GS_URL)).thenThrow(new IllegalStateException());
        LoadData<InputStream> result = loaderWithMockStorage().buildLoadData(VALID_GS_URL, 0, 0, new Options());
        assertNull(result);
    }

    // Factory

    @Test
    public void factory_noArg_buildsStringLoader() {
        // Verify the no-arg Factory produces a non-null loader without throwing
        FirebaseImageLoader.StringLoader loader =
                (FirebaseImageLoader.StringLoader) new FirebaseImageLoader.StringLoader.Factory().build(null);
        assertNotNull(loader);
    }

    @Test
    public void factory_withStorage_buildsStringLoaderUsingProvidedInstance() {
        FirebaseImageLoader.StringLoader loader =
                (FirebaseImageLoader.StringLoader) new FirebaseImageLoader.StringLoader.Factory(mStorage).build(null);
        // The provided storage instance should be used — valid URL resolves without exception
        LoadData<InputStream> result = loader.buildLoadData(VALID_GS_URL, 0, 0, new Options());
        assertNotNull(result);
    }

    private FirebaseImageLoader.StringLoader loaderWithMockStorage() {
        return new FirebaseImageLoader.StringLoader(mStorage);
    }
}
